package io.onedev.server.web.page.my.password;

import io.onedev.commons.utils.ExplicitException;
import io.onedev.server.model.User;
import io.onedev.server.web.component.user.passwordedit.PasswordEditPanel;
import io.onedev.server.web.page.my.MyPage;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class MyPasswordPage extends MyPage {
	
	public MyPasswordPage(PageParameters params) {
		super(params);
		if (getLoginUser().getPassword() == null)
			throw new ExplicitException("Unable to change password as you are authenticating via external system");
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new PasswordEditPanel("content", new AbstractReadOnlyModel<User>() {

				@Override
				public User getObject() {
					return getLoginUser();
				}
				
			}));
	}

	@Override
	protected Component newTopbarTitle(String componentId) {
		return new Label(componentId, "Change My Password");
	}

}
