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
package org.jtalks.poulpe.service.transactional;

import org.jtalks.common.model.entity.ComponentType;
import org.jtalks.poulpe.model.dao.BranchDao;
import org.jtalks.poulpe.model.dao.ComponentDao;
import org.jtalks.poulpe.model.dao.SectionDao;
import org.jtalks.poulpe.model.entity.Jcommune;
import org.jtalks.poulpe.model.entity.PoulpeBranch;
import org.jtalks.poulpe.model.entity.PoulpeSection;
import org.jtalks.poulpe.service.ForumStructureService;
import org.jtalks.poulpe.service.JCommuneNotifier;
import org.jtalks.poulpe.service.exceptions.JcommuneRespondedWithErrorException;
import org.jtalks.poulpe.service.exceptions.JcommuneUrlNotConfiguredException;
import org.jtalks.poulpe.service.exceptions.NoConnectionToJcommuneException;

import java.util.List;

/**
 * @author stanislav bashkirtsev
 * @author Guram Savinov
 */
public class TransactionalForumStructureService implements ForumStructureService {
    private final SectionDao sectionDao;
    private final BranchDao branchDao;
    private final ComponentDao componentDao;
    private final JCommuneNotifier jCommuneNotifier;

    /** Constructor for initialization variables */
    public TransactionalForumStructureService(SectionDao sectionDao, BranchDao branchDao, ComponentDao componentDao,
            JCommuneNotifier jCommuneNotifier) {
        this.sectionDao = sectionDao;
        this.branchDao = branchDao;
        this.componentDao = componentDao;
        this.jCommuneNotifier = jCommuneNotifier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveJcommune(Jcommune jcommune) {
        componentDao.saveOrUpdate(jcommune);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Jcommune getJcommune() {
        return (Jcommune) componentDao.getByType(ComponentType.FORUM);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeBranch(PoulpeBranch branch)
        throws NoConnectionToJcommuneException,JcommuneRespondedWithErrorException,JcommuneUrlNotConfiguredException {
        Jcommune jcommune = (Jcommune) componentDao.getByType(ComponentType.FORUM);
        jCommuneNotifier.notifyAboutBranchDelete(jcommune.getUrl(), branch);
        PoulpeSection section = (PoulpeSection) branch.getSection();
        section.deleteBranch(branch);
        sectionDao.saveOrUpdate(section);
        branchDao.delete(branch);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveBranch(PoulpeBranch branch, PoulpeSection toSection) {
        PoulpeSection fromSection = branch.moveTo(toSection);
        sectionDao.saveOrUpdate(fromSection);
        sectionDao.saveOrUpdate(toSection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Jcommune deleteSectionWithBranches(PoulpeSection section)
        throws NoConnectionToJcommuneException,JcommuneRespondedWithErrorException,JcommuneUrlNotConfiguredException {
        Jcommune jcommune = (Jcommune) componentDao.getByType(ComponentType.FORUM);
        jCommuneNotifier.notifyAboutSectionDelete(jcommune.getUrl(), section);
        jcommune.removeSection(section);
        componentDao.saveOrUpdate(jcommune);
        return jcommune;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteSectionAndMoveBranches(PoulpeSection toRemove, PoulpeSection toReceiveBranches) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PoulpeBranch saveBranch(PoulpeSection inSection, PoulpeBranch notYetSavedBranch) {
        if (notYetSavedBranch.getSection() == null) {
            inSection.addOrUpdateBranch(notYetSavedBranch);
            notYetSavedBranch.setSection(inSection);
            sectionDao.saveOrUpdate(inSection);
        } else {
            moveBranch(notYetSavedBranch, inSection);
        }
        return notYetSavedBranch;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void moveBranch(PoulpeBranch branch, PoulpeBranch target) {
        PoulpeSection targetSection = target.getPoulpeSection();
        List<PoulpeBranch> branchesOfTargetSection = targetSection.getPoulpeBranches();
        int index = branchesOfTargetSection.indexOf(target);
        PoulpeSection sectionBranchWasRemovedFrom = branch.removeFromSection();
        targetSection.addBranchIfAbsentTo(index, branch);
        sectionDao.saveOrUpdate(targetSection);
        sectionDao.saveOrUpdate(sectionBranchWasRemovedFrom);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveSection(PoulpeSection section, PoulpeSection target) {
        Jcommune jcommune = getJcommune();
        jcommune.moveSection(section, target);
        componentDao.saveOrUpdate(jcommune);
    }
}
