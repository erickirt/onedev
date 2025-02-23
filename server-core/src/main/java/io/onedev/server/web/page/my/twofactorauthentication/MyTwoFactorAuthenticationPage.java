package io.onedev.server.web.page.my.twofactorauthentication;

import io.onedev.commons.utils.ExplicitException;
import io.onedev.server.model.User;
import io.onedev.server.web.component.user.twofactorauthentication.TwoFactorAuthenticationStatusPanel;
import io.onedev.server.web.page.my.MyPage;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class MyTwoFactorAuthenticationPage extends MyPage {

	public MyTwoFactorAuthenticationPage(PageParameters params) {
		super(params);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		if (!getLoginUser().isEnforce2FA())
			throw new ExplicitException("Two-factor authentication not enabled");
		
		add(new TwoFactorAuthenticationStatusPanel("content") {
			@Override
			protected User getUser() {
				return getLoginUser();
			}
		});
	}

	@Override
	protected Component newTopbarTitle(String componentId) {
		return new Label(componentId, "Two Factor Authentication");
	}

}
