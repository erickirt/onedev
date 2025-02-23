package io.onedev.server.web.component.user.accesstoken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.AccessTokenManager;
import io.onedev.server.model.AccessToken;
import io.onedev.server.model.User;

public abstract class AccessTokenListPanel extends Panel {

	private WebMarkupContainer container;
	
	public AccessTokenListPanel(String id) {
		super(id);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		container = new WebMarkupContainer("tokens");
		container.setOutputMarkupId(true);
		add(container);
		container.add(new ListView<>("tokens", new AbstractReadOnlyModel<List<AccessToken>>() {

			@Override
			public List<AccessToken> getObject() {
				var tokens = new ArrayList<>(getUser().getAccessTokens());
				Collections.sort(tokens);
				return tokens;
			}
			
		}) {

			private Component newViewer(String componentId, Long tokenId) {
				return new AccessTokenPanel(componentId) {

					@Override
					protected void onDelete(AjaxRequestTarget target) {
						getTokenManager().delete(getToken());
						target.add(container);
					}

					@Override
					protected void onEdit(AjaxRequestTarget target) {
						AccessTokenEditPanel editor = new AccessTokenEditPanel("token") {

							private void view(AjaxRequestTarget target) {
								Component viewer = newViewer(componentId, tokenId);
								replaceWith(viewer);
								target.add(viewer);
							}

							@Override
							protected AccessToken getToken() {
								return getTokenManager().load(tokenId);
							}

							@Override
							protected void onSaved(AjaxRequestTarget target) {
								view(target);
							}

							@Override
							protected void onCancelled(AjaxRequestTarget target) {
								view(target);
							}

						};
						replaceWith(editor);
						target.add(editor);
					}

					@Override
					protected AccessToken getToken() {
						return getTokenManager().load(tokenId);
					}

				};
			}
			
			@Override
			protected void populateItem(final ListItem<AccessToken> item) {
				item.add(newViewer("token", item.getModelObject().getId()));
			}

		});
		
		container.add(newAddNewFrag());
	}

	private Component newAddNewFrag() {
		Fragment fragment = new Fragment("newToken", "addNewLinkFrag", this);
		fragment.add(new AjaxLink<Void>("link") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				Component editor = new AccessTokenEditPanel("newToken") {

					@Override
					protected AccessToken getToken() {
						var token = new AccessToken();
						token.setOwner(getUser());
						return token;
					}

					@Override
					protected void onSaved(AjaxRequestTarget target) {
						container.replace(newAddNewFrag());
						target.add(container);
					}

					@Override
					protected void onCancelled(AjaxRequestTarget target) {
						Component newAddNewFrag = newAddNewFrag();
						container.replace(newAddNewFrag);
						target.add(newAddNewFrag);
					}

				};
				container.replace(editor);
				target.add(editor);
			}

		});
		fragment.setOutputMarkupId(true);
		return fragment;
	}
	
	protected abstract User getUser();
	
	private AccessTokenManager getTokenManager() {
		return OneDev.getInstance(AccessTokenManager.class);
	}
	
}
