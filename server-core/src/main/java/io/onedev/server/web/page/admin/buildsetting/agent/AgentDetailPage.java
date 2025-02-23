package io.onedev.server.web.page.admin.buildsetting.agent;

import io.onedev.commons.utils.ExplicitException;
import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.AgentManager;
import io.onedev.server.model.Agent;
import io.onedev.server.web.component.tabbable.PageTab;
import io.onedev.server.web.component.tabbable.Tab;
import io.onedev.server.web.component.tabbable.Tabbable;
import io.onedev.server.web.page.admin.AdministrationPage;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

public abstract class AgentDetailPage extends AdministrationPage {

	public static final String PARAM_AGENT = "agent";
	
	protected final IModel<Agent> agentModel;
	
	public AgentDetailPage(PageParameters params) {
		super(params);

		String agentName = params.get(PARAM_AGENT).toString();
		
		Agent agent = OneDev.getInstance(AgentManager.class).findByName(agentName);
		
		if (agent == null) 
			throw new ExplicitException("Unable to find agent " + agentName);
		
		Long agentId = agent.getId();
		
		agentModel = new LoadableDetachableModel<Agent>() {

			@Override
			protected Agent load() {
				return OneDev.getInstance(AgentManager.class).load(agentId);
			}
			
		};
		
		// we do not need to reload the project this time as we already have that object on hand
		agentModel.setObject(agent);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		List<Tab> tabs = new ArrayList<>();
		
		tabs.add(new PageTab(Model.of("Overview"), AgentOverviewPage.class, AgentOverviewPage.paramsOf(getAgent())));
		tabs.add(new PageTab(Model.of("Builds"), AgentBuildsPage.class, AgentBuildsPage.paramsOf(getAgent())));
		tabs.add(new PageTab(Model.of("Log"), AgentLogPage.class, AgentLogPage.paramsOf(getAgent())));
		
		add(new Tabbable("agentTabs", tabs).setOutputMarkupId(true));
	}

	protected Agent getAgent() {
		return agentModel.getObject();
	}
	
	@Override
	protected void onDetach() {
		agentModel.detach();
		super.onDetach();
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new AgentCssResourceReference()));
	}

	@Override
	protected Component newTopbarTitle(String componentId) {
		return new Label(componentId, getAgent().getName());
	}

	public static PageParameters paramsOf(Agent agent) {
		PageParameters params = new PageParameters();
		params.add(PARAM_AGENT, agent.getName());
		return params;
	}
	
}