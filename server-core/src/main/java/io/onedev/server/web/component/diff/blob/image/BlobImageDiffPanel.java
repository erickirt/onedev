package io.onedev.server.web.component.diff.blob.image;

import io.onedev.server.git.Blob;
import io.onedev.server.git.BlobChange;
import io.onedev.server.git.BlobIdent;
import io.onedev.server.util.Provider;
import io.onedev.server.web.page.base.BasePage;
import io.onedev.server.web.resource.RawBlobResource;
import io.onedev.server.web.resource.RawBlobResourceReference;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.resource.PackageResourceReference;

public class BlobImageDiffPanel extends Panel {

	private final BlobChange change;
	
	public BlobImageDiffPanel(String id, BlobChange change) {
		super(id);
	
		this.change = change;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(newImage("old", change.getOldBlobIdent(), new Provider<Blob>() {

			@Override
			public Blob get() {
				return change.getOldBlob();
			}
			
		}));
		
		add(newImage("new", change.getNewBlobIdent(), new Provider<Blob>() {

			@Override
			public Blob get() {
				return change.getNewBlob();
			}
			
		}));
		
		add(AttributeAppender.append("class", "border border-top-0 rounded-bottom blob-image-diff d-flex"));
	}
	
	private Image newImage(String id, BlobIdent blobIdent, Provider<Blob> blobProvider) {
		Image image;
		if (blobIdent.path != null) {
			add(image = new Image(id, new RawBlobResourceReference(), 
					RawBlobResource.paramsOf(change.getProject(), blobIdent)));
		} else {
			BasePage page = (BasePage) getPage();
			String blank = page.isDarkMode()?"blank-dark.png":"blank.png";
			add(image = new Image(id, new PackageResourceReference(BlobImageDiffPanel.class, blank)));
		}
		return image;
	}

}
