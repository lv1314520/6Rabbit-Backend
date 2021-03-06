package com.rabbit.backend.Service;

import com.rabbit.backend.Bean.Attach.Attach;
import com.rabbit.backend.Bean.Attach.AttachUploadForm;
import com.rabbit.backend.Bean.Attach.ThreadAttach;
import com.rabbit.backend.Bean.Attach.ThreadAttachForm;
import com.rabbit.backend.DAO.AttachDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

@Service
public class AttachService {
    @Value("${rabbit.path}")
    private String basePath;

    private AttachDAO attachDAO;

    @Autowired
    public AttachService(AttachDAO attachDAO) {
        this.attachDAO = attachDAO;
    }

    public String getRealPath(String fileName) {
        return basePath + "attach/" + fileName;
    }

    private Boolean deleteByFile(File file, String aid) {
        if (file.exists() && file.delete()) {
            attachDAO.delete(aid);
            attachDAO.deleteCASCADE(aid);
            return true;
        } else {
            return false;
        }
    }

    @Transactional
    public Boolean deleteByAid(String aid) {
        Attach attach = attachDAO.find(aid);
        File file = new File(getRealPath(attach.getFileName()));
        return deleteByFile(file, aid);
    }

    @Transactional
    public Boolean deleteByAttach(Attach attach) {
        File file = new File(getRealPath(attach.getFileName()));
        return deleteByFile(file, attach.getAid());
    }

    public List<Attach> list(String tid) {
        return attachDAO.findByTid(tid);
    }

    public List<ThreadAttach> listWithoutUser(String pid) {
        return attachDAO.findByPidWithoutUser(pid);
    }

    @Transactional
    public Boolean deleteByTid(String tid) {
        List<Attach> attachList = attachDAO.findByTid(tid);
        int failedCount = 0;
        for (Attach attach : attachList) {
            if (!deleteByAttach(attach)) {
                failedCount++;
            }
        }
        return failedCount == 0;
    }

    @Transactional
    public Boolean deleteByPid(String pid) {
        List<Attach> attachList = attachDAO.findByPid(pid);
        int failedCount = 0;
        for (Attach attach : attachList) {
            if (!deleteByAttach(attach)) {
                failedCount++;
            }
        }
        return failedCount == 0;
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * ?")
    public void deleteAllUnused() {
        List<Attach> attachList = attachDAO.findAllUnused();
        for (Attach attach : attachList) {
            deleteByAttach(attach);
        }
    }

    public void updateAttachThread(String aid, String tid, String pid, Integer creditsType, Integer credits) {
        attachDAO.updateAttachThread(aid, tid, pid, creditsType, credits);
    }

    public Integer threadAttachCount(String tid) {
        return attachDAO.threadAttachCount(tid);
    }

    public Integer userUnusedAttachCount(String uid) {
        return attachDAO.userUnusedCount(uid);
    }

    public String uid(String aid) {
        return attachDAO.uid(aid);
    }

    public String insert(AttachUploadForm attachUploadForm) {
        attachDAO.insert(attachUploadForm);
        return attachUploadForm.getAid();
    }

    public void updateAttach(AttachUploadForm attachUploadForm) {
        attachDAO.updateAttach(attachUploadForm);
    }

    public List<Attach> findUnused(String uid) {
        return attachDAO.findUnused(uid);
    }

    private void attachThread(String aid, String tid, String pid, Integer creditsType, Integer credits) {
        attachDAO.updateAttachThread(aid, tid, pid, creditsType, credits);
    }

    private Boolean attachAccess(String aid, String pid, String uid) {
        Attach attach = attachDAO.find(aid);
        return (attach.getTid() == null || attach.getPid().equals(pid)) && attach.getUser().getUid().equals(uid);
    }

    public void batchAttachThread(List<ThreadAttachForm> attachList, String tid, String pid, String uid) {
        if (attachList != null && attachList.size() != 0) {
            for (ThreadAttachForm attachForm : attachList) {
                if (this.attachAccess(attachForm.getAid(), pid, uid)) {
                    this.attachThread(attachForm.getAid(), tid, pid,
                            attachForm.getCreditsType(), attachForm.getCredits());
                }
            }
        }
    }

    public Attach find(String aid) {
        return attachDAO.find(aid);
    }

    public ThreadAttach findWithThreadAttach(String aid) {
        return attachDAO.findWithThreadAttach(aid);
    }

    @Async
    @Transactional
    public void incrementDownloads(String aid) {
        attachDAO.incrementDownloads(aid);
    }
}
