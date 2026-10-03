package com.example.appmgmt.service.application;

import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.ApplicationVersion;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.StatusCd;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 申込メニューの領域（手続きの区切り）。0 = 新規申込、1 以降 = 契約変更 N。
 * 版の並びから組み立てる：版種別 3（契約変更）の版が現れるたびに新しい契約変更が始まる。
 */
public class PhaseGroup {
    public enum State { ACTIVE, COMPLETED, CANCELED }

    private final int index;
    private final List<ApplicationVersion> versions = new ArrayList<>();
    private ApplicationVersion displayVersion;
    private ApplicationVersion baseline;
    private boolean current;
    private State state;

    private PhaseGroup(int index) {
        this.index = index;
    }

    /** 版の一覧（版番号順）から領域を組み立てる。版がなければ新規申込の領域だけを返す。 */
    public static List<PhaseGroup> build(Application app, List<ApplicationVersion> allVersions) {
        List<ApplicationVersion> sorted = new ArrayList<>(allVersions);
        sorted.sort(Comparator.comparingInt(ApplicationVersion::getVersionNo));
        List<PhaseGroup> groups = new ArrayList<>();
        PhaseGroup g = new PhaseGroup(0);
        groups.add(g);
        for (ApplicationVersion v : sorted) {
            if (Codes.VERSION_CHANGE.equals(v.getVersionType())) {
                g = new PhaseGroup(groups.size());
                groups.add(g);
            }
            g.versions.add(v);
        }
        String st = app.getStatusCd();
        for (PhaseGroup pg : groups) {
            ApplicationVersion last = pg.versions.isEmpty() ? null : pg.versions.get(pg.versions.size() - 1);
            ApplicationVersion lastLive = null;
            boolean allCanceled = !pg.versions.isEmpty();
            for (ApplicationVersion v : pg.versions) {
                if (!"1".equals(v.getCanceledFlg())) {
                    lastLive = v;
                    allCanceled = false;
                }
                if (v.getVersionNo() == app.getCurrentVersionNo()) {
                    pg.current = true;
                }
            }
            pg.displayVersion = lastLive != null ? lastLive : last;
            if (pg.index == 0) {
                if (StatusCd.isCanceled(st)) {
                    pg.state = State.CANCELED;
                } else if (pg.current && !StatusCd.REVIEWED.equals(st)) {
                    pg.state = State.ACTIVE;
                } else {
                    pg.state = State.COMPLETED;
                }
            } else if (allCanceled) {
                pg.state = State.CANCELED;
            } else if (pg.current && !StatusCd.CHG_REVIEWED.equals(st)) {
                pg.state = State.ACTIVE;
            } else {
                pg.state = State.COMPLETED;
            }
        }
        // 契約変更の比較元（変更前）：直前に審査完了した領域の表示版。なければ新規申込の表示版
        for (int i = 1; i < groups.size(); i++) {
            ApplicationVersion base = groups.get(0).displayVersion;
            for (int j = i - 1; j >= 1; j--) {
                if (groups.get(j).state == State.COMPLETED) {
                    base = groups.get(j).displayVersion;
                    break;
                }
            }
            groups.get(i).baseline = base;
        }
        return groups;
    }

    /** 現行版を含む領域（見つからなければ先頭）。 */
    public static PhaseGroup currentOf(List<PhaseGroup> groups) {
        for (PhaseGroup g : groups) {
            if (g.current) {
                return g;
            }
        }
        return groups.get(0);
    }

    public static PhaseGroup find(List<PhaseGroup> groups, Integer index) {
        if (index == null || index < 0 || index >= groups.size()) {
            return currentOf(groups);
        }
        return groups.get(index);
    }

    public int getIndex() { return index; }
    public boolean isContractChange() { return index > 0; }
    public String getTitle() { return index == 0 ? "新規申込" : "契約変更" + index; }
    public List<ApplicationVersion> getVersions() { return versions; }
    public ApplicationVersion getDisplayVersion() { return displayVersion; }
    /** 契約変更の変更前（審査完了版）。新規申込では null。 */
    public ApplicationVersion getBaseline() { return baseline; }
    public boolean isCurrent() { return current; }
    public State getState() { return state; }
    public String getStateName() {
        switch (state) {
            case ACTIVE: return "進行中";
            case COMPLETED: return index == 0 ? "審査完了" : "審査完了";
            default: return index == 0 ? "取消" : "取消（差戻し・取消）";
        }
    }
    public Set<Integer> versionNos() {
        Set<Integer> s = new LinkedHashSet<>();
        for (ApplicationVersion v : versions) {
            s.add(v.getVersionNo());
        }
        return s;
    }
    public boolean contains(int versionNo) { return versionNos().contains(versionNo); }
    public int getFirstVersionNo() { return versions.isEmpty() ? 0 : versions.get(0).getVersionNo(); }
    public int getLastVersionNo() { return versions.isEmpty() ? 0 : versions.get(versions.size() - 1).getVersionNo(); }
}
