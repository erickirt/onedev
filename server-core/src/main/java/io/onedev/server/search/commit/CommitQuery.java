package io.onedev.server.search.commit;

import static io.onedev.commons.codeassist.FenceAware.unfence;
import static io.onedev.commons.utils.StringUtils.unescape;
import static org.apache.commons.lang3.StringUtils.join;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.jgit.lib.ObjectId;

import io.onedev.commons.utils.ExplicitException;
import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.BuildManager;
import io.onedev.server.entityreference.BuildReference;
import io.onedev.server.event.project.RefUpdated;
import io.onedev.server.git.command.RevListOptions;
import io.onedev.server.model.Build;
import io.onedev.server.model.Project;
import io.onedev.server.search.commit.CommitQueryParser.CriteriaContext;

public class CommitQuery implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private final List<CommitCriteria> criterias;
	
	public CommitQuery(List<CommitCriteria> criterias) {
		this.criterias = criterias;
	}
	
	public static CommitQuery parse(Project project, @Nullable String queryString, boolean withCurrentUserCriteria) {
		List<CommitCriteria> criterias = new ArrayList<>();
		if (queryString != null) {
			CharStream is = CharStreams.fromString(queryString); 
			CommitQueryLexer lexer = new CommitQueryLexer(is);
			lexer.removeErrorListeners();
			lexer.addErrorListener(new BaseErrorListener() {

				@Override
				public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
						int charPositionInLine, String msg, RecognitionException e) {
					throw new RuntimeException("Malformed query", e);
				}
				
			});
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			CommitQueryParser parser = new CommitQueryParser(tokens);
			parser.removeErrorListeners();
			parser.setErrorHandler(new BailErrorStrategy());
			
			List<String> authorValues = new ArrayList<>();
			List<String> committerValues = new ArrayList<>();
			List<String> beforeValues = new ArrayList<>();
			List<String> afterValues = new ArrayList<>();
			List<String> pathValues = new ArrayList<>();
			List<String> messageValues = new ArrayList<>();
			List<String> fuzzyValues = new ArrayList<>();
			List<Revision> revisions = new ArrayList<>();
			
			for (CriteriaContext criteria: parser.query().criteria()) {
				if (criteria.authorCriteria() != null) {
					if (criteria.authorCriteria().AuthoredByMe() != null) {
						if (!withCurrentUserCriteria)
							throw new ExplicitException("Criteria '" + criteria.authorCriteria().AuthoredByMe().getText() + "' is not supported here");
						authorValues.add(null);
					} else {
						for (var value: criteria.authorCriteria().Value())
							authorValues.add(getValue(value));
					}
				} else if (criteria.committerCriteria() != null) {
					if (criteria.committerCriteria().CommittedByMe() != null) {
						if (!withCurrentUserCriteria)
							throw new ExplicitException("Criteria '" + criteria.committerCriteria().CommittedByMe().getText() + "' is not supported here");
						committerValues.add(null);
					} else {
						for (var value: criteria.committerCriteria().Value())
							committerValues.add(getValue(value));
					}
				} else if (criteria.messageCriteria() != null) {
					for (var value: criteria.messageCriteria().Value()) 
						messageValues.add(getValue(value));
				} else if (criteria.fuzzyCriteria() != null) {
					fuzzyValues.add(unescape(unfence(criteria.fuzzyCriteria().getText())));
				} else if (criteria.pathCriteria() != null) {
					for (var value: criteria.pathCriteria().Value())
						pathValues.add(getValue(value));
				} else if (criteria.beforeCriteria() != null) {
					beforeValues.add(getValue(criteria.beforeCriteria().Value()));
				} else if (criteria.afterCriteria() != null) {
					afterValues.add(getValue(criteria.afterCriteria().Value()));
				} else if (criteria.revisionCriteria() != null) {
					List<String> values = new ArrayList<>();
					Revision.Scope scope;
					if (criteria.revisionCriteria().DefaultBranch() != null) {
						values.add(project.getDefaultBranch());
					} else {
						for (var valueNode: criteria.revisionCriteria().Value()) {
							var value = getValue(valueNode);
							if (criteria.revisionCriteria().BUILD() != null) {
								var buildReference = BuildReference.of(value, project);
								Build build = OneDev.getInstance(BuildManager.class).find(buildReference.getProject(), buildReference.getNumber());
								if (build == null)
									throw new ExplicitException("Unable to find build: " + value);
								else
									value = build.getCommitHash();
							}
							values.add(value);
						}
					}
					if (criteria.revisionCriteria().SINCE() != null)
						scope = Revision.Scope.SINCE;
					else if (criteria.revisionCriteria().UNTIL() != null)
						scope = Revision.Scope.UNTIL;
					else
						scope = null;
					for (var value: values) 
						revisions.add(new Revision(value, scope, criteria.revisionCriteria().getText()));
				}
			}
			
			if (!authorValues.isEmpty())
				criterias.add(new AuthorCriteria(authorValues));
			if (!committerValues.isEmpty())
				criterias.add(new CommitterCriteria(committerValues));
			if (!pathValues.isEmpty())
				criterias.add(new PathCriteria(pathValues));
			if (!messageValues.isEmpty())
				criterias.add(new MessageCriteria(messageValues));
			if (!beforeValues.isEmpty())
				criterias.add(new BeforeCriteria(beforeValues));
			if (!afterValues.isEmpty())
				criterias.add(new AfterCriteria(afterValues));
			if (!revisions.isEmpty())
				criterias.add(new RevisionCriteria(revisions));
			if (!fuzzyValues.isEmpty())
				criterias.add(new FuzzyCriteria(fuzzyValues));
		}
		
		return new CommitQuery(criterias);
	}
	
	private static String getValue(TerminalNode valueNode) {
		return unescape(unfence(valueNode.getText())); 
	}
	
	public boolean matches(RefUpdated event) {
		if (!event.getNewCommitId().equals(ObjectId.zeroId())) 
			return criterias.stream().allMatch(it->it.matches(event));
		else 
			return false;
	}
	
	public void fill(Project project, RevListOptions options) {
		criterias.stream().forEach(it->it.fill(project, options));
	}
	
	public List<CommitCriteria> getCriterias() {
		return criterias;
	}

	public static CommitQuery merge(CommitQuery query1, CommitQuery query2) {
		List<CommitCriteria> criterias = new ArrayList<>();
		criterias.addAll(query1.getCriterias());
		criterias.addAll(query2.getCriterias());
		return new CommitQuery(criterias);
	}

	@Override
	public String toString() {
		List<String> parts = new ArrayList<>();
		for (CommitCriteria criteria: criterias)
			parts.add(criteria.toString());
		return join(parts, " ");
	}
	
}
