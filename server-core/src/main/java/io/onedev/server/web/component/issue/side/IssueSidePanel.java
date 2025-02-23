package io.onedev.server.web.component.issue.side;

import com.google.common.collect.Lists;
import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.*;
import io.onedev.server.entityreference.EntityReference;
import io.onedev.server.model.*;
import io.onedev.server.model.support.EntityWatch;
import io.onedev.server.persistence.TransactionManager;
import io.onedev.server.search.entity.EntityQuery;
import io.onedev.server.search.entity.issue.IssueQuery;
import io.onedev.server.search.entity.issue.IssueQueryLexer;
import io.onedev.server.search.entity.issue.IssueQueryParseOption;
import io.onedev.server.search.entity.issue.StateCriteria;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.util.Input;
import io.onedev.server.util.LinkSide;
import io.onedev.server.util.Similarities;
import io.onedev.server.util.criteria.Criteria;
import io.onedev.server.web.WebConstants;
import io.onedev.server.web.ajaxlistener.AttachAjaxIndicatorListener;
import io.onedev.server.web.ajaxlistener.ConfirmClickListener;
import io.onedev.server.web.behavior.ChangeObserver;
import io.onedev.server.web.component.entity.reference.EntityReferencePanel;
import io.onedev.server.web.component.entity.watches.EntityWatchesPanel;
import io.onedev.server.web.component.floating.AlignPlacement;
import io.onedev.server.web.component.issue.IssueStateBadge;
import io.onedev.server.web.component.issue.choice.IssueAddChoice;
import io.onedev.server.web.component.issue.choice.IssueChoiceProvider;
import io.onedev.server.web.component.issue.create.CreateIssuePanel;
import io.onedev.server.web.component.issue.fieldvalues.FieldValuesPanel;
import io.onedev.server.web.component.issue.operation.TransitionMenuLink;
import io.onedev.server.web.component.issue.statestats.StateStatsBar;
import io.onedev.server.web.component.iteration.IterationStatusLabel;
import io.onedev.server.web.component.iteration.choice.AbstractIterationChoiceProvider;
import io.onedev.server.web.component.iteration.choice.IterationChoiceResourceReference;
import io.onedev.server.web.component.link.ViewStateAwarePageLink;
import io.onedev.server.web.component.modal.ModalLink;
import io.onedev.server.web.component.modal.ModalPanel;
import io.onedev.server.web.component.select2.Response;
import io.onedev.server.web.component.select2.ResponseFiller;
import io.onedev.server.web.component.select2.SelectToActChoice;
import io.onedev.server.web.component.user.ident.Mode;
import io.onedev.server.web.component.user.ident.UserIdentPanel;
import io.onedev.server.web.component.user.list.SimpleUserListLink;
import io.onedev.server.web.editable.InplacePropertyEditLink;
import io.onedev.server.web.page.base.BasePage;
import io.onedev.server.web.page.project.issues.detail.IssueActivitiesPage;
import io.onedev.server.web.page.project.issues.iteration.IterationIssuesPage;
import io.onedev.server.web.page.simple.security.LoginPage;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.annotation.Nullable;
import javax.mail.internet.InternetAddress;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static io.onedev.server.search.entity.issue.IssueQuery.parse;
import static io.onedev.server.security.SecurityUtils.*;
import static io.onedev.server.util.EmailAddressUtils.describe;

public abstract class IssueSidePanel extends Panel {

	private static final int MAX_DISPLAY_AVATARS = 20;
	
	private final IModel<List<LinkSpec>> linkSpecsModel = new LoadableDetachableModel<>() {
		@Override
		protected List<LinkSpec> load() {
			return getLinkSpecManager().queryAndSort();
		}

	};
	
	private boolean confidential;
	
	private Component watchesContainer;
	
	private boolean showAllLinks;
	
	public IssueSidePanel(String id) {
		super(id);
		confidential = getIssue().isConfidential();
	}

	@Override
	protected void onBeforeRender() {
		addOrReplace(newFieldsContainer());
		addOrReplace(newConfidentialContainer());
		addOrReplace(newIterationsContainer());
		addOrReplace(newLinksContainer());
		addOrReplace(newVotesContainer());
		
		addOrReplace(watchesContainer = new EntityWatchesPanel("watches") {

			@Override
			protected void onSaveWatch(EntityWatch watch) {
				OneDev.getInstance(IssueWatchManager.class).createOrUpdate((IssueWatch) watch);
			}

			@Override
			protected void onDeleteWatch(EntityWatch watch) {
				OneDev.getInstance(IssueWatchManager.class).delete((IssueWatch) watch);
			}

			@Override
			protected AbstractEntity getEntity() {
				return getIssue();
			}

			@Override
			protected boolean isAuthorized(User user) {
				return canAccessIssue(user.asSubject(), getIssue());
			}
			
		});
		addOrReplace(newExternalParticipantsContainer());
		
		addOrReplace(new EntityReferencePanel("reference") {

			@Override
			protected EntityReference getReference() {
				return getIssue().getReference();
			}
			
		});
		
		if (SecurityUtils.canManageIssues(getProject())) 
			addOrReplace(newDeleteLink("delete"));		
		else 
			addOrReplace(new WebMarkupContainer("delete").setVisible(false));
		
		super.onBeforeRender();
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		add(new AjaxLink<Void>("showAllLinks") {
			@Override
			protected void onInitialize() {
				super.onInitialize();
				add(new Label("label", new LoadableDetachableModel<String>() {

					@Override
					protected String load() {
						var hasVisibleLinks = false;
						for (LinkSpec spec : linkSpecsModel.getObject()) {
							if (canEditIssueLink(getProject(), spec) && spec.isShowAlways()
									|| getIssue().getLinks().stream().anyMatch(it -> it.getSpec().equals(spec))) {
								hasVisibleLinks = true;
								break;
							}
						}
						if (hasVisibleLinks)
							return "More Links";
						else
							return "Show Links";
					}
					
				}));
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				showAllLinks = true;
				target.add(IssueSidePanel.this);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				if (showAllLinks) {
					setVisible(false);
				} else {
					var hasLinksToShow = false;
					for (LinkSpec spec : linkSpecsModel.getObject()) {
						if (canEditIssueLink(getProject(), spec) && !spec.isShowAlways()
								&& getIssue().getLinks().stream().noneMatch(it -> it.getSpec().equals(spec))) {
							hasLinksToShow = true;
							break;
						}
					}
					setVisible(hasLinksToShow);
				}
			}
		});

		add(new ChangeObserver() {
			
			@Override
			public Collection<String> findObservables() {
				return Lists.newArrayList(Issue.getDetailChangeObservable(getIssue().getId()));
			}
			
		});
		
		setOutputMarkupId(true);
	}

	@Override
	protected void onDetach() {
		linkSpecsModel.detach();
		super.onDetach();
	}

	private Component newFieldsContainer() {
		IModel<List<Input>> fieldsModel = new LoadableDetachableModel<List<Input>>() {

			@Override
			protected List<Input> load() {
				List<Input> fields = new ArrayList<>();
				for (Input field: getIssue().getFieldInputs().values()) {
					if (getIssue().isFieldVisible(field.getName()))
						fields.add(field);
				}
				return fields;
			}
			
		};		
		return new ListView<Input>("fields", fieldsModel) {

			@Override
			protected void populateItem(ListItem<Input> item) {
				Input field = item.getModelObject();
				item.add(new Label("name", field.getName()));
				item.add(new FieldValuesPanel("values", Mode.NAME, false) {

					@Override
					protected AttachAjaxIndicatorListener getInplaceEditAjaxIndicator() {
						return new AttachAjaxIndicatorListener(false);
					}

					@Override
					protected Issue getIssue() {
						return IssueSidePanel.this.getIssue();
					}

					@Override
					protected Input getField() {
						return item.getModelObject();
					}
					
				}.setRenderBodyOnly(true));
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!fieldsModel.getObject().isEmpty());
			}
			
		};
	}
	
	private Component newConfidentialContainer() {
		CheckBox confidentialInput = new CheckBox("confidential", new PropertyModel<Boolean>(this, "confidential"));
		confidentialInput.add(new AjaxFormComponentUpdatingBehavior("change") {
			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				
				String precondition = "" +
						"if (onedev.server.form.confirmLeave())" +
						"	return true;" +
						"if ($(this).is(':checkbox'))" +
						"  this.checked = !this.checked;" +
						"return false;";
				attributes.getAjaxCallListeners().add(new AjaxCallListener().onPrecondition(precondition));
			}

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				OneDev.getInstance(IssueChangeManager.class).changeConfidential(getIssue(), confidential);
				setResponsePage(getPage());
			}
			
		});
		confidentialInput.setVisible(SecurityUtils.canModifyIssue(getIssue()));
		
		return confidentialInput;
	}
	
	private Component newLinksContainer() {
		return new ListView<LinkSide>("links", new LoadableDetachableModel<>() {

			@Override
			protected List<LinkSide> load() {
				List<LinkSide> links = new ArrayList<>();
				for (LinkSpec spec : linkSpecsModel.getObject()) {
					if (canEditIssueLink(getProject(), spec) && (spec.isShowAlways() || showAllLinks)
							|| getIssue().getLinks().stream().anyMatch(it -> it.getSpec().equals(spec) && (it.getSource().equals(getIssue()) || canAccessIssue(it.getSource())) && (it.getTarget().equals(getIssue()) || canAccessIssue(it.getTarget())))) {
						if (spec.getOpposite() != null) {
							IssueQuery query = spec.getOpposite().getParsedIssueQuery(getProject());
							if (query.matches(getIssue()))
								links.add(new LinkSide(spec, false));
							query = spec.getParsedIssueQuery(getProject());
							if (query.matches(getIssue()))
								links.add(new LinkSide(spec, true));
						} else {
							IssueQuery query = spec.getParsedIssueQuery(getProject());
							if (query.matches(getIssue()))
								links.add(new LinkSide(spec, false));
						}
					}
				}
				return links;
			}

		}) {

			@Override
			protected void populateItem(ListItem<LinkSide> item) {
				LinkSide side = item.getModelObject();
				
				if (side.isOpposite() && side.getSpec().getOpposite().isMultiple() 
						|| !side.isOpposite() && side.getSpec().isMultiple()) {
					item.add(newMultipleLinks(item.getModel()));
				} else {
					item.add(newSingleLink(item.getModel()));
				}
			}
			
			private Fragment newMultipleLinks(IModel<LinkSide> model) {
				Fragment fragment = new Fragment("content", "multipleLinksFrag", IssueSidePanel.this);
				LinkSide side = model.getObject();
				LinkSpec spec = side.getSpec();
				boolean opposite = side.isOpposite();
				
				boolean canEditIssueLink = canEditIssueLink(getProject(), spec);
				
				String name = spec.getName(opposite);
				fragment.add(new Label("name", name));
				
				RepeatingView linkedIssuesView = new RepeatingView("linkedIssues");
				for (Issue linkedIssue: getIssue().findLinkedIssues(spec, opposite)) {
					LinkDeleteListener deleteListener;
					if (canEditIssueLink) { 
						deleteListener = new LinkDeleteListener() {
	
							@Override
							void onDelete(AjaxRequestTarget target, Issue linkedIssue) {
								getIssueChangeManager().removeLink(model.getObject().getSpec(), getIssue(), 
										linkedIssue, opposite);
								notifyIssueChange(target, getIssue());
							}
							
						};
					} else {
						deleteListener = null;
					}
					linkedIssuesView.add(newLinkedIssueContainer(linkedIssuesView.newChildId(), 
							linkedIssue, deleteListener));
				}
				fragment.add(linkedIssuesView);

				fragment.add(new IssueAddChoice("linkNew", new IssueChoiceProvider() {

					@Override
					protected Project getProject() {
						return getIssue().getProject();
					}
					
					@Override
					protected EntityQuery<Issue> getScope() {
						LinkSpec spec = model.getObject().getSpec();
						if (opposite) 
							return spec.getOpposite().getParsedIssueQuery(getProject());
						else 
							return spec.getParsedIssueQuery(getProject());
					}
					
				}) {

					@Override
					protected void onSelect(AjaxRequestTarget target, Issue selection) {
						LinkSpec spec = model.getObject().getSpec();
						if (getIssue().equals(selection)) {
							getSession().warn("Can not link to self");
						} else if (getIssue().findLinkedIssues(spec, opposite).contains(selection)) { 
							getSession().warn("Issue already added");
						} else {
							getIssueChangeManager().addLink(spec, getIssue(), selection, opposite);
							notifyIssueChange(target, getIssue());
						}
					}
					
				}.setVisible(canEditIssueLink));

				fragment.add(new ModalLink("createNew") {

					@Override
					protected Component newContent(String id, ModalPanel modal) {
						return new CreateIssuePanel(id) {

							@Nullable
							@Override
							protected Criteria<Issue> getTemplate() {
								String query;
								var spec = model.getObject().getSpec();
								if (opposite)
									query = spec.getOpposite().getIssueQuery();
								else
									query = spec.getIssueQuery();
								return parse(getProject(), query, new IssueQueryParseOption(), false).getCriteria();
							}

							@Override
							protected void onSave(AjaxRequestTarget target, Issue issue) {
								getIssueManager().open(issue);
								notifyIssueChange(target, issue);
								getIssueChangeManager().addLink(model.getObject().getSpec(), 
										getIssue(), issue, opposite);
								notifyIssueChange(target, getIssue());
								modal.close();
							}

							@Override
							protected void onCancel(AjaxRequestTarget target) {
								modal.close();
							}

							@Override
							protected Project getProject() {
								return getIssue().getProject();
							}

						};
					}
					
				}.setVisible(canEditIssueLink));
				
				return fragment;
			}
			
			private Fragment newSingleLink(IModel<LinkSide> model) {
				Fragment fragment = new Fragment("content", "singleLinkFrag", IssueSidePanel.this);
				LinkSide side = model.getObject();
				fragment.add(new Label("name", side.getSpec().getName(side.isOpposite())));
				
				SingleLinkBean bean = new SingleLinkBean();
				
				Issue prevLinkedIssue = getIssue().findLinkedIssue(side.getSpec(), side.isOpposite());
				if (prevLinkedIssue != null)
					bean.setIssueId(prevLinkedIssue.getId());
				
				Long prevLinkedIssueId = bean.getIssueId();
				
				boolean authorized = canEditIssueLink(getProject(), side.getSpec());
				fragment.add(new InplacePropertyEditLink("edit", new AlignPlacement(100, 0, 100, 0)) {

					@Override
					protected Serializable getBean() {
						return bean;
					}

					@Override
					protected String getPropertyName() {
						return "issueId";
					}

					@Override
					protected Project getProject() {
						return getIssue().getProject();
					}

					@Override
					protected IssueQuery getIssueQuery() {
						LinkSide side = model.getObject();
						if (side.isOpposite()) 
							return side.getSpec().getOpposite().getParsedIssueQuery(getProject());
						else 
							return side.getSpec().getParsedIssueQuery(getProject());
					}

					@Override
					protected void onUpdated(IPartialPageRequestHandler handler, Serializable bean,
							String propertyName) {
						LinkSide side = model.getObject();
						SingleLinkBean singleLinkBean = (SingleLinkBean) bean;
						Issue linkedIssue = null;
						if (singleLinkBean.getIssueId() != null) 
							linkedIssue = getIssueManager().load(singleLinkBean.getIssueId());
						if (getIssue().equals(linkedIssue)) {
							getSession().warn("Can not link to self");
							singleLinkBean.setIssueId(prevLinkedIssueId);
						} else {
							Issue prevLinkedIssue = getIssue().findLinkedIssue(side.getSpec(), side.isOpposite());
							getIssueChangeManager().changeLink(side.getSpec(), getIssue(), prevLinkedIssue, linkedIssue, side.isOpposite());
							notifyIssueChange(handler, getIssue());
						}
					}

				}.setVisible(authorized));
				
				if (prevLinkedIssue != null) 
					fragment.add(newLinkedIssueContainer("body", prevLinkedIssue, null));
				else 
					fragment.add(new WebMarkupContainer("body").setVisible(false));
				
				return fragment;
			}
			
		};
	}
	
	private Component newLinkedIssueContainer(String componentId, Issue linkedIssue, 
			@Nullable LinkDeleteListener deleteListener) {
		if (canAccessIssue(linkedIssue)) {
			Long linkedIssueId = linkedIssue.getId();
			Fragment fragment = new Fragment(componentId, "linkedIssueFrag", this);
			Link<Void> link = new BookmarkablePageLink<Void>("reference", IssueActivitiesPage.class, 
					IssueActivitiesPage.paramsOf(linkedIssue));
			link.add(new Label("label", linkedIssue.getReference().toString(getProject())));
			fragment.add(link);
			
			fragment.add(new AjaxLink<Void>("delete") {

				@Override
				protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
					super.updateAjaxAttributes(attributes);
					attributes.getAjaxCallListeners().add(new ConfirmClickListener(
							"Do you really want to remove this link?"));
				}

				@Override
				public void onClick(AjaxRequestTarget target) {
					Issue linkedIssue = getIssueManager().load(linkedIssueId);
					deleteListener.onDelete(target, linkedIssue);
					notifyIssueChange(target, getIssue());
				}
				
			}.setVisible(deleteListener != null));

			AjaxLink<Void> stateLink = new TransitionMenuLink("state") {

				@Override
				protected Issue getIssue() {
					return getIssueManager().load(linkedIssueId);
				}

			};

			stateLink.add(new IssueStateBadge("badge", new LoadableDetachableModel<>() {
				@Override
				protected Issue load() {
					return getIssueManager().load(linkedIssueId);
				}
			}, true).add(AttributeAppender.append("class", "badge-sm")));
			
			fragment.add(stateLink);

			link = new BookmarkablePageLink<Void>("title", IssueActivitiesPage.class, 
					IssueActivitiesPage.paramsOf(linkedIssue));
			link.add(new Label("label", linkedIssue.getTitle()));
			fragment.add(link);
			
			return fragment;
		} else {
			return new WebMarkupContainer(componentId).setVisible(false);
		}
	}
	
	private Component newIterationsContainer() {
		WebMarkupContainer container = new WebMarkupContainer("iterations") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!getIssue().getSchedules().isEmpty() || SecurityUtils.canScheduleIssues(getProject()));
			}
			
		};
		
		container.add(new ListView<Iteration>("iterations", new AbstractReadOnlyModel<List<Iteration>>() {

			@Override
			public List<Iteration> getObject() {
				return getIssue().getIterations().stream()
						.sorted(new Iteration.DatesAndStatusComparator())
						.collect(Collectors.toList()); 
			}
			
		}) {

			@Override
			protected void populateItem(ListItem<Iteration> item) {
				Iteration iteration = item.getModelObject();

				Link<Void> link = new BookmarkablePageLink<Void>("link", IterationIssuesPage.class, 
						IterationIssuesPage.paramsOf(getIssue().getProject(), iteration, null));
				link.add(new Label("label", iteration.getName()));
				item.add(link);
				
				item.add(new StateStatsBar("progress", new AbstractReadOnlyModel<Map<String, Integer>>() {

					@Override
					public Map<String, Integer> getObject() {
						return item.getModelObject().getStateStats(getIssue().getProject());
					}
					
				}) {

					@Override
					protected Link<Void> newStateLink(String componentId, String state) {
						String query = new IssueQuery(new StateCriteria(state, IssueQueryLexer.Is)).toString();
						PageParameters params = IterationIssuesPage.paramsOf(getIssue().getProject(), 
								item.getModelObject(), query);
						return new ViewStateAwarePageLink<Void>(componentId, IterationIssuesPage.class, params);
					}
					
				});
				item.add(new IterationStatusLabel("status", new AbstractReadOnlyModel<Iteration>() {

					@Override
					public Iteration getObject() {
						return item.getModelObject();
					}
					
				}).add(AttributeAppender.append("class", "badge-sm")));
				
				item.add(new AjaxLink<Void>("delete") {

					@Override
					protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
						super.updateAjaxAttributes(attributes);
						if (!getIssue().isNew()) {
							attributes.getAjaxCallListeners().add(new ConfirmClickListener("Do you really want to "
									+ "remove the issue from iteration '" + item.getModelObject().getName() + "'?"));
						}
					}
					
					@Override
					public void onClick(AjaxRequestTarget target) {
						getIssueChangeManager().removeSchedule(getIssue(), item.getModelObject());
						notifyIssueChange(target, getIssue());
					}
					
					@Override
					protected void onConfigure() {
						super.onConfigure();
						setVisible(SecurityUtils.canScheduleIssues(getIssue().getProject()));
					}
					
				});
			}
			
		});
		
		container.add(new SelectToActChoice<Iteration>("add", new AbstractIterationChoiceProvider() {
			
			@Override
			public void query(String term, int page, Response<Iteration> response) {
				List<Iteration> iterations = getProject().getSortedHierarchyIterations();
				iterations.removeAll(getIssue().getIterations());
				
				iterations = new Similarities<Iteration>(iterations) {

					@Override
					public double getSimilarScore(Iteration object) {
						return Similarities.getSimilarScore(object.getName(), term);
					}
					
				};
				new ResponseFiller<>(response).fill(iterations, page, WebConstants.PAGE_SIZE);
			}
			
		}) {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				
				getSettings().setPlaceholder("Add to iteration...");
				getSettings().setFormatResult("onedev.server.iterationChoiceFormatter.formatResult");
				getSettings().setFormatSelection("onedev.server.iterationChoiceFormatter.formatSelection");
				getSettings().setEscapeMarkup("onedev.server.iterationChoiceFormatter.escapeMarkup");
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.canScheduleIssues(getIssue().getProject()));
			}

			@Override
			public void renderHead(IHeaderResponse response) {
				super.renderHead(response);
				response.render(JavaScriptHeaderItem.forReference(new IterationChoiceResourceReference()));
			}
			
			@Override
			protected void onSelect(AjaxRequestTarget target, Iteration iteration) {
				getIssueChangeManager().addSchedule(getIssue(), iteration);
				notifyIssueChange(target, getIssue());
			}

		});		
		
		return container;
	}
	
	private List<IssueVote> getSortedVotes() {
		List<IssueVote> votes = new ArrayList<>(getIssue().getVotes());
		Collections.sort(votes, new Comparator<IssueVote>() {

			@Override
			public int compare(IssueVote o1, IssueVote o2) {
				return o2.getId().compareTo(o1.getId());
			}
			
		});
		return votes;
	}
	
	private Component newVotesContainer() {
		WebMarkupContainer container = new WebMarkupContainer("votes");
		container.setOutputMarkupId(true);

		container.add(new Label("count", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return String.valueOf(getIssue().getVoteCount());
			}
			
		}));

		container.add(new ListView<>("voters", new LoadableDetachableModel<List<IssueVote>>() {

			@Override
			protected List<IssueVote> load() {
				List<IssueVote> votes = getSortedVotes();
				if (votes.size() > MAX_DISPLAY_AVATARS)
					votes = votes.subList(0, MAX_DISPLAY_AVATARS);
				return votes;
			}

		}) {

			@Override
			protected void populateItem(ListItem<IssueVote> item) {
				User user = item.getModelObject().getUser();
				item.add(new UserIdentPanel("voter", user, Mode.AVATAR));
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!getIssue().getVotes().isEmpty());
			}

		});
		
		container.add(new SimpleUserListLink("more") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getIssue().getVotes().size() > MAX_DISPLAY_AVATARS);
			}

			@Override
			protected List<User> getUsers() {
				List<IssueVote> votes = getSortedVotes();
				if (votes.size() > MAX_DISPLAY_AVATARS)
					votes = votes.subList(MAX_DISPLAY_AVATARS, votes.size());
				else
					votes = new ArrayList<>();
				return votes.stream().map(it->it.getUser()).collect(Collectors.toList());
			}
					
		});
		
		AjaxLink<Void> voteLink = new AjaxLink<Void>("vote") {

			private IssueVote getVote(User user) {
				for (IssueVote vote: getIssue().getVotes()) {
					if (user.equals(vote.getUser())) 
						return vote;
				}
				return null;
			}
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				if (SecurityUtils.getAuthUser() != null) {
					IssueVote vote = getVote(SecurityUtils.getAuthUser());
					if (vote == null) {
						vote = new IssueVote();
						vote.setIssue(getIssue());
						vote.setUser(SecurityUtils.getAuthUser());
						vote.setDate(new Date());
						OneDev.getInstance(IssueVoteManager.class).create(vote);
						getIssue().getVotes().add(vote);
						target.add(watchesContainer);
					} else {
						getIssue().getVotes().remove(vote);
						OneDev.getInstance(IssueVoteManager.class).delete(vote);
					}
					target.add(container);
				} else {
					throw new RestartResponseAtInterceptPageException(LoginPage.class);
				}
			}

			@Override
			protected void onInitialize() {
				super.onInitialize();
				add(new Label("label", new LoadableDetachableModel<String>() {

					@Override
					protected String load() {
						if (SecurityUtils.getAuthUser() != null) {
							if (getVote(SecurityUtils.getAuthUser()) != null)
								return "Unvote";
							else
								return "Vote";
						} else {
							return "Login to vote";
						}
					}
					
				}));
			}
			
		};
		container.add(voteLink);
		
		return container;
	}

	private Component newExternalParticipantsContainer() {
		WebMarkupContainer container = new WebMarkupContainer("externalParticipants") {
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!getIssue().getExternalParticipants().isEmpty());
			}
			
		};
		container.setOutputMarkupId(true);

		container.add(new ListView<>("externalParticipants", new LoadableDetachableModel<List<InternetAddress>>() {

			@Override
			protected List<InternetAddress> load() {
				var addresses = new ArrayList<>(getIssue().getExternalParticipants());
				addresses.sort((o1, o2) -> describe(o1, canManageIssues(getProject())).compareTo(describe(o2, canManageIssues(getProject()))));
				return addresses;
			}
			
		}) {

			@Override
			protected void populateItem(ListItem<InternetAddress> item) {
				item.add(new Label("label", describe(item.getModelObject(), canManageIssues(getProject()))));
				item.add(new AjaxLink<Void>("delete") {
					@Override
					protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
						super.updateAjaxAttributes(attributes);
						attributes.getAjaxCallListeners().add(new ConfirmClickListener("'" + describe(item.getModelObject(), canManageIssues(getProject())) + "' will not be able to participate in this issue. Do you want to continue?"));
					}

					@Override
					protected void onConfigure() {
						super.onConfigure();
						setVisible(SecurityUtils.canManageIssues(getIssue().getProject()));
					}
					
					@Override
					public void onClick(AjaxRequestTarget target) {
						OneDev.getInstance(TransactionManager.class).run(() -> {
							getIssue().getExternalParticipants().remove(item.getModelObject());
						});		
						target.add(container);
					}
				});
			}
			
		});

		return container;
	}
	
	private Project getProject() {
		return getIssue().getProject();
	}
	
	private IssueChangeManager getIssueChangeManager() {
		return OneDev.getInstance(IssueChangeManager.class);
	}
	
	private IssueManager getIssueManager() {
		return OneDev.getInstance(IssueManager.class);
	}
	
	private LinkSpecManager getLinkSpecManager() {
		return OneDev.getInstance(LinkSpecManager.class);
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new IssueSideCssResourceReference()));
	}

	protected abstract Issue getIssue();

	protected abstract Component newDeleteLink(String componentId);

	private void notifyIssueChange(IPartialPageRequestHandler handler, Issue issue) {
		((BasePage)getPage()).notifyObservablesChange(handler, issue.getChangeObservables(true));
	}
	
	private static abstract class LinkDeleteListener implements Serializable {
		
		abstract void onDelete(AjaxRequestTarget target, Issue linkedIssue);
		
	}
}
