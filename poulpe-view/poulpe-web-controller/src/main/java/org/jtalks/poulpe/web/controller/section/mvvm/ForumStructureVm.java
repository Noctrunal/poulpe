/**
 * Copyright (C) 2011  JTalks.org Team
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jtalks.poulpe.web.controller.section.mvvm;

import org.jtalks.common.model.entity.ComponentType;
import org.jtalks.poulpe.model.entity.Jcommune;
import org.jtalks.poulpe.model.entity.PoulpeSection;
import org.jtalks.poulpe.service.ComponentService;
import org.jtalks.poulpe.web.controller.section.TreeNodeFactory;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zul.DefaultTreeModel;
import org.zkoss.zul.TreeModel;

import javax.validation.constraints.NotNull;

/**
 * Is used in order to work with page that allows admin to manage sections and branches (moving them, reordering,
 * removing, editing, etc.).
 *
 * @author stanislav bashkirtsev
 */
public class ForumStructureVm {
    private final ComponentService componentService;
    private Jcommune jcommune;
    private boolean showCreateSectionDialog;
    private PoulpeSection selectedSection;

    public ForumStructureVm(@NotNull ComponentService componentService) {
        this.componentService = componentService;
    }

    /**
     * First of all decides whether to show a dialog for creation of the entity or for editing by looking at {@link
     * #getSelectedSection()} and then changes the flag {@link #isShowCreateSectionDialog()} in order to open the
     * editing dialog.
     */
    @Command
    @NotifyChange({"showCreateSectionDialog", "selectedSection"})
    public void showNewSectionDialog() {
        showCreateSectionDialog = true;
        if (isCreatingNewSection()) {
            selectedSection = new PoulpeSection();
        }
    }

    /**
     * While opening a dialog we need to know whether the dialog is for creating a new section or editing the existing
     * one.
     *
     * @return {@code true} if there are no existing sections being selected for editing
     */
    private boolean isCreatingNewSection() {
        return selectedSection == null || selectedSection.getId() == 0;
    }

    /**
     * Saves the {@link #getSelectedSection()} to the database, adds it as the last one to the list of sections and
     * cleans the selected section. Also makes the create section dialog to be closed.
     */
    @Command
    @NotifyChange({"sections", "showCreateSectionDialog", "selectedSection"})
    public void saveSection() {
        jcommune.addSection(selectedSection);
        componentService.saveComponent(jcommune);
        selectedSection = null;
        showCreateSectionDialog = false;
    }

    /**
     * Hits the database to obtain all the sections in the forum.
     */
    @Init
    public void initForumStructure() {
        jcommune = getJcommune();
    }

    /**
     * Returns all the sections in our database in order they are actually sorted.
     *
     * @return all the sections in our database in order they are actually sorted or empty list if there are no
     *         sections. Can't return {@code null}.
     */
    @SuppressWarnings("unchecked")
    public TreeModel getSections() {
        return new DefaultTreeModel(TreeNodeFactory.buildForumStructure(jcommune));
    }

    /**
     * Decides whether the  Edit Section dialog should be shown. It's bound to the ZK Window on ZUL page by {@code
     * visible="@bind(...)"}. You should use this in order to control the window visibility. Use NotifyChanges in order
     * ZUL to understand that this field was changed.
     *
     * @return {@code true} if the dialog should be shown to the user
     */
    public boolean isShowCreateSectionDialog() {
        return showCreateSectionDialog;
    }

    /**
     * Defines whether the selected item is branch at the moment. Is needed for instance to decide whether to shown some
     * context menu items or not.
     *
     * @return {@code true} if the selected item is branch, {@code false} if nothing is selected or the selected items
     *         is a section
     */
    public boolean isBranchSelected() {
        return false;
    }

    /**
     * Let's ZK binder know what section the edit section dialog should work with. It's changed by {@link
     * #setSelectedSection(PoulpeSection)} or it's a newly created section if the one is being created (see {@link
     * #showNewSectionDialog()} for more details).
     *
     * @return currently selected (or newly created) section to be filled by edit section dialog
     */
    public PoulpeSection getSelectedSection() {
        return selectedSection;
    }

    /**
     * Is used by ZK binder to inject the section that is currently selected.
     *
     * @param selectedSection the section that is currently selected
     */
    public void setSelectedSection(PoulpeSection selectedSection) {
        this.selectedSection = selectedSection;
    }

    private Jcommune getJcommune() {
        return (Jcommune) componentService.getByType(ComponentType.FORUM);
    }
}