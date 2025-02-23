package io.onedev.server.web.page.project.builds.detail.issues;

import io.onedev.server.model.Build;
import io.onedev.server.model.Project;
import io.onedev.server.search.entity.issue.FixedInBuildCriteria;
import io.onedev.server.search.entity.issue.IssueQuery;
import io.onedev.server.web.component.issue.list.IssueListPanel;
import io.onedev.server.web.page.project.builds.detail.BuildDetailPage;
import io.onedev.server.web.util.paginghistory.PagingHistorySupport;
import io.onedev.server.web.util.paginghistory.ParamPagingHistorySupport;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;

public class FixedIssuesPage extends BuildDetailPage {

	private static final String PARAM_QUERY = "query";
	
	private static final String PARAM_PAGE = "page";

	private String query;
	
	private IssueListPanel issueList;
	
	public FixedIssuesPage(PageParameters params) {
		super(params);
		query = params.get(PARAM_QUERY).toString();
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		if (getBuild().getStreamPrevious(Build.Status.SUCCESSFUL) != null) {
			Fragment fragment = new Fragment("content", "hasPreviousSuccessfulBuildFrag", this);
			
			fragment.add(issueList = new IssueListPanel("issues", new IModel<String>() {

				@Override
				public void detach() {
				}

				@Override
				public String getObject() {
					return query;
				}

				@Override
				public void setObject(String object) {
					query = object;
					PageParameters params = getPageParameters();
					params.set(PARAM_QUERY, query);
					params.remove(PARAM_PAGE);
					CharSequence url = RequestCycle.get().urlFor(FixedIssuesPage.class, params);
					pushState(RequestCycle.get().find(AjaxRequestTarget.class), url.toString(), query);
				}
				
			}) {

				@Override
				protected IssueQuery getBaseQuery() {
					return new IssueQuery(new FixedInBuildCriteria(getBuild()), new ArrayList<>());
				}
				
				@Override
				protected PagingHistorySupport getPagingHistorySupport() {
					return new ParamPagingHistorySupport() {
						
						@Override
						public PageParameters newPageParameters(int currentPage) {
							PageParameters params = paramsOf(getBuild(), query);
							params.add(PARAM_PAGE, currentPage+1);
							return params;
						}
						
						@Override
						public int getCurrentPage() {
							return getPageParameters().get(PARAM_PAGE).toInt(1)-1;
						}
						
					};
				}

				@Override
				protected Project getProject() {
					return FixedIssuesPage.this.getProject();
				}
				
			});
			
			add(fragment);
		} else {
			add(new Fragment("content", "noPreviousSuccessfulBuildFrag", this));
		}
	}

	@Override
	protected void onPopState(AjaxRequestTarget target, Serializable data) {
		query = (String) data;
		getPageParameters().set(PARAM_QUERY, query);
		target.add(issueList);
	}

	public static PageParameters paramsOf(Build build, @Nullable String query) {
		PageParameters params = paramsOf(build);
		if (query != null)
			params.add(PARAM_QUERY, query);
		return params;
	}
	
}
