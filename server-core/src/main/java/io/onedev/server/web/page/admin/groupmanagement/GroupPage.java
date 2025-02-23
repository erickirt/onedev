package io.onedev.server.web.page.admin.groupmanagement;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.GroupManager;
import io.onedev.server.model.Group;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.web.component.tabbable.PageTab;
import io.onedev.server.web.component.tabbable.Tabbable;
import io.onedev.server.web.page.admin.AdministrationPage;
import io.onedev.server.web.page.admin.groupmanagement.authorization.GroupAuthorizationsPage;
import io.onedev.server.web.page.admin.groupmanagement.membership.GroupMembershipsPage;
import io.onedev.server.web.page.admin.groupmanagement.profile.GroupProfilePage;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

public abstract class GroupPage extends AdministrationPage {
	
	public static final String PARAM_GROUP = "group";
	
	protected final IModel<Group> groupModel;
	
	public GroupPage(PageParameters params) {
		super(params);
		
		String groupIdString = params.get(PARAM_GROUP).toString();
		if (StringUtils.isBlank(groupIdString))
			throw new RestartResponseException(GroupListPage.class);
		
		Long groupId = Long.valueOf(groupIdString);
		
		groupModel = new LoadableDetachableModel<Group>() {

			@Override
			protected Group load() {
				return OneDev.getInstance(GroupManager.class).load(groupId);
			}
			
		};
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		List<PageTab> tabs = new ArrayList<>();
		
		var params = paramsOf(getGroup());
		tabs.add(new PageTab(Model.of("Profile"), Model.of("profile"), GroupProfilePage.class, params));
		tabs.add(new PageTab(Model.of("Members"), Model.of("members"), GroupMembershipsPage.class, params));
		tabs.add(new PageTab(Model.of("Authorized Projects"), Model.of("project"), GroupAuthorizationsPage.class, params));
		
		add(new Tabbable("groupTabs", tabs));
	}
	
	@Override
	protected void onDetach() {
		groupModel.detach();
		
		super.onDetach();
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new GroupCssResourceReference()));
	}
	
	public Group getGroup() {
		return groupModel.getObject();
	}
	
	@Override
	protected boolean isPermitted() {
		return SecurityUtils.isAdministrator();
	}
	
	public static PageParameters paramsOf(Group group) {
		PageParameters params = new PageParameters();
		params.add(PARAM_GROUP, group.getId());
		return params;
	}

	@Override
	protected Component newTopbarTitle(String componentId) {
		Fragment fragment = new Fragment(componentId, "topbarTitleFrag", this);
		fragment.add(new BookmarkablePageLink<Void>("groups", GroupListPage.class));
		fragment.add(new Label("groupName", getGroup().getName()));
		return fragment;
	}

}
