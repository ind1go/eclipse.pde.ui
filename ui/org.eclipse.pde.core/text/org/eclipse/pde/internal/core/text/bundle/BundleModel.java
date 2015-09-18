/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class BundleModel extends AbstractEditingModel implements IBundleModel {

	private IBundle fBundle;
	private IBundleModelFactory fFactory;

	/**
	 * @param document
	 * @param isReconciling
	 */
	public BundleModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
		fBundle = new Bundle(this);
	}

	@Override
	protected NLResourceHelper createNLResourceHelper() {
		return null;
	}

	@Override
	public boolean isFragmentModel() {
		return getBundle().getHeader(Constants.FRAGMENT_HOST) != null;
	}

	@Override
	public void load(InputStream source, boolean outOfSync) throws CoreException {
		try {
			fLoaded = true;
			((Bundle) getBundle()).load(ManifestElement.parseBundleManifest(source, null));
		} catch (BundleException e) {
			fLoaded = false;
		} catch (IOException e) {
			fLoaded = false;
		}
	}

	@Override
	public void adjustOffsets(IDocument document) {
		((Bundle) getBundle()).clearOffsets();
		((Bundle) getBundle()).adjustOffsets(document);
	}

	@Override
	public IBundle getBundle() {
		return fBundle;
	}

	@Override
	public IBundleModelFactory getFactory() {
		if (fFactory == null)
			fFactory = new BundleModelFactory(this);
		return fFactory;
	}

}
