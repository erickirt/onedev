package io.onedev.server.web.page.my;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.SettingManager;
import io.onedev.server.model.User;
import io.onedev.server.web.page.layout.LayoutPage;
import io.onedev.server.web.page.simple.security.LoginPage;
import io.onedev.server.web.util.UserAware;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public abstract class MyPage extends LayoutPage implements UserAware {
	
	public MyPage(PageParameters params) {
		super(params);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		if (getUser() == null) 
			throw new RestartResponseAtInterceptPageException(LoginPage.class);
	}

	@Override
	public User getUser() {
		return getLoginUser();
	}

	@Override
	protected String getPageTitle() {
		return "My - " + OneDev.getInstance(SettingManager.class).getBrandingSetting().getName();
	}
	
}
