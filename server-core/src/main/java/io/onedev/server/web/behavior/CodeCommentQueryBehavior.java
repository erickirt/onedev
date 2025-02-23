package io.onedev.server.web.behavior;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.onedev.commons.codeassist.FenceAware;
import io.onedev.commons.codeassist.InputCompletion;
import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.codeassist.grammar.LexerRuleRefElementSpec;
import io.onedev.commons.codeassist.parser.Element;
import io.onedev.commons.codeassist.parser.ParseExpect;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.commons.utils.ExplicitException;
import io.onedev.server.model.CodeComment;
import io.onedev.server.model.Project;
import io.onedev.server.search.entity.codecomment.CodeCommentQuery;
import io.onedev.server.search.entity.codecomment.CodeCommentQueryParser;
import io.onedev.server.search.entity.project.ProjectQuery;
import io.onedev.server.util.DateUtils;
import io.onedev.server.web.behavior.inputassist.ANTLRAssistBehavior;
import io.onedev.server.web.util.SuggestionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

import static io.onedev.server.search.entity.codecomment.CodeCommentQuery.getRuleName;
import static io.onedev.server.search.entity.codecomment.CodeCommentQueryLexer.*;

public class CodeCommentQueryBehavior extends ANTLRAssistBehavior {

	private static final String FUZZY_SUGGESTION_DESCRIPTION_PREFIX = "enclose with ~";
	
	private final IModel<Project> projectModel;
	
	private final boolean withCurrentUserCriteria;
	
	private final boolean withOrder;
	
	public CodeCommentQueryBehavior(IModel<Project> projectModel, boolean withCurrentUserCriteria, boolean withOrder) {
		super(CodeCommentQueryParser.class, "query", false);
		this.projectModel = projectModel;
		this.withCurrentUserCriteria = withCurrentUserCriteria;
		this.withOrder = withOrder;
	}
	
	@Override
	public void detach(Component component) {
		super.detach(component);
		projectModel.detach();
	}
	
	private Project getProject() {
		return projectModel.getObject();
	}
	
	@Override
	protected List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
		if (terminalExpect.getElementSpec() instanceof LexerRuleRefElementSpec) {
			LexerRuleRefElementSpec spec = (LexerRuleRefElementSpec) terminalExpect.getElementSpec();
			if (spec.getRuleName().equals("Quoted")) {
				return new FenceAware(codeAssist.getGrammar(), '"', '"') {

					@Override
					protected List<InputSuggestion> match(String matchWith) {
						Project project = getProject();
						ParseExpect criteriaValueExpect;
						if ("criteriaField".equals(spec.getLabel())) {
							return SuggestionUtils.suggest(CodeComment.QUERY_FIELDS, matchWith);
						} else if ("orderField".equals(spec.getLabel())) {
							return SuggestionUtils.suggest(new ArrayList<>(CodeComment.SORT_FIELDS.keySet()), matchWith);
						} else if ((criteriaValueExpect = terminalExpect.findExpectByLabel("criteriaValue")) != null) {
							List<Element> fieldElements = criteriaValueExpect.getState().findMatchedElementsByLabel("criteriaField", true);
							List<Element> operatorElements = criteriaValueExpect.getState().findMatchedElementsByLabel("operator", true);
							Preconditions.checkState(operatorElements.size() == 1);
							String operatorName = StringUtils.normalizeSpace(operatorElements.get(0).getMatchedText());
							int operator = CodeCommentQuery.getOperator(operatorName);							
							if (fieldElements.isEmpty()) {
								if (operator == Mentioned || operator == CreatedBy || operator == RepliedBy) 
									return SuggestionUtils.suggestUsers(matchWith);
								else 
									return null;
							} else {
								String fieldName = CodeCommentQuery.getValue(fieldElements.get(0).getMatchedText());
								try {
									CodeCommentQuery.checkField(project, fieldName, operator);
									if (fieldName.equals(CodeComment.NAME_CREATE_DATE) 
											|| fieldName.equals(CodeComment.NAME_LAST_ACTIVITY_DATE)) {
										List<InputSuggestion> suggestions = SuggestionUtils.suggest(DateUtils.RELAX_DATE_EXAMPLES, matchWith);
										return !suggestions.isEmpty()? suggestions: null;
									} else if (fieldName.equals(CodeComment.NAME_PATH)) {
										return SuggestionUtils.suggestBlobs(projectModel.getObject(), matchWith);
									} else {
										return null;
									}
								} catch (ExplicitException ignored) {
								}
							}
						}
						return new ArrayList<>();
					}
					
					@Override
					protected String getFencingDescription() {
						return "value should be quoted";
					}
					
				}.suggest(terminalExpect);
			} else if (spec.getRuleName().equals("Fuzzy")) {
				return new FenceAware(codeAssist.getGrammar(), '~', '~') {

					@Override
					protected List<InputSuggestion> match(String matchWith) {
						return null;
					}

					@Override
					protected String getFencingDescription() {
						return FUZZY_SUGGESTION_DESCRIPTION_PREFIX + " to query path/content/reply";
					}

				}.suggest(terminalExpect);
			}
		} 
		return null;
	}
	
	@Override
	protected Optional<String> describe(ParseExpect parseExpect, String suggestedLiteral) {
		if (!withOrder && suggestedLiteral.equals(getRuleName(OrderBy))
				|| !withCurrentUserCriteria && (suggestedLiteral.equals(getRuleName(CreatedByMe)) || suggestedLiteral.equals(getRuleName(RepliedByMe)) || suggestedLiteral.equals(getRuleName(MentionedMe)))) {
			return null;
		} else if (suggestedLiteral.equals(",")) {
			return Optional.of("add another value");
		}
		
		parseExpect = parseExpect.findExpectByLabel("operator");
		if (parseExpect != null) {
			List<Element> fieldElements = parseExpect.getState().findMatchedElementsByLabel("criteriaField", false);
			if (!fieldElements.isEmpty()) {
				String fieldName = CodeCommentQuery.getValue(fieldElements.iterator().next().getMatchedText());
				try {
					CodeCommentQuery.checkField(getProject(), fieldName, CodeCommentQuery.getOperator(suggestedLiteral));
				} catch (ExplicitException e) {
					return null;
				}
			}
		}
		return super.describe(parseExpect, suggestedLiteral);
	}

	@Override
	protected List<String> getHints(TerminalExpect terminalExpect) {
		List<String> hints = new ArrayList<>();
		if (terminalExpect.getElementSpec() instanceof LexerRuleRefElementSpec) {
			LexerRuleRefElementSpec spec = (LexerRuleRefElementSpec) terminalExpect.getElementSpec();
			if ("criteriaValue".equals(spec.getLabel()) && ProjectQuery.isInsideQuote(terminalExpect.getUnmatchedText())) {
				List<Element> fieldElements = terminalExpect.getState().findMatchedElementsByLabel("criteriaField", true);
				if (!fieldElements.isEmpty()) {
					String fieldName = ProjectQuery.getValue(fieldElements.get(0).getMatchedText());
					if (fieldName.equals(CodeComment.NAME_CONTENT)) {
						hints.add("Use '*' for wildcard match");
						hints.add("Use '\\' to escape quotes");
					}
				}
			}
		} 
		return hints;
	}

	@Override
	protected boolean isFuzzySuggestion(InputCompletion suggestion) {
		return suggestion.getDescription() != null 
				&& suggestion.getDescription().startsWith(FUZZY_SUGGESTION_DESCRIPTION_PREFIX);
	}
	
}
