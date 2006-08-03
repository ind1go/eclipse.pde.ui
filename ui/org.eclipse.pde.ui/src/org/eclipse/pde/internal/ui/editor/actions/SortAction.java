/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.pde.internal.ui.PDEPluginImages;

public class SortAction extends Action {

	private boolean fSorted = false;
	private StructuredViewer fViewer;
	private ViewerSorter fSorter;

	public SortAction(StructuredViewer viewer, String tooltipText, ViewerSorter sorter, IPropertyChangeListener listener) {
		super(tooltipText, IAction.AS_CHECK_BOX);
		setToolTipText(tooltipText);
		setImageDescriptor(PDEPluginImages.DESC_ALPHAB_SORT_CO);
		fSorted = viewer.getSorter() == null ? false : true;
		setChecked(fSorted);
		fViewer= viewer;
		fSorter = sorter != null ? sorter : new ViewerSorter();
		if (listener != null)
			addListenerObject(listener);
	}

	public void run() {
		fViewer.setSorter(fSorted ? null : fSorter);
		fSorted = !fSorted;
	}

}
