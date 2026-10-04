package com.example.appmgmt.domain;

import java.util.ArrayList;
import java.util.List;

/** 同意事項（M_CONSENT_DOCUMENT）。申込同意確認画面（AP03）に PDF で表示する文書。 */
public class ConsentDocument extends AuditedEntity {
    private String documentCd;
    private String documentName;
    private String targetType;
    private int displayOrder;
    private String validFlg;
    /** 表示用：適用中の版（なければ null）と、全版（版番号の降順）。 */
    private ConsentDocumentVersion currentVersion;
    private List<ConsentDocumentVersion> versions = new ArrayList<>();
    /** 表示用：この同意で適用中の版を開いたか（AP03）。 */
    private boolean viewed;

    public String getDocumentCd() { return documentCd; }
    public void setDocumentCd(String documentCd) { this.documentCd = documentCd; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetTypeName() { return Codes.label("DOC_TARGET", targetType); }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public String getValidFlg() { return validFlg; }
    public void setValidFlg(String validFlg) { this.validFlg = validFlg; }
    public boolean isValid() { return Codes.FLG_ON.equals(validFlg); }
    public ConsentDocumentVersion getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(ConsentDocumentVersion currentVersion) { this.currentVersion = currentVersion; }
    public List<ConsentDocumentVersion> getVersions() { return versions; }
    public void setVersions(List<ConsentDocumentVersion> versions) { this.versions = versions; }
    public boolean isViewed() { return viewed; }
    public void setViewed(boolean viewed) { this.viewed = viewed; }

    /** 同意種別（1：新規申込、2：契約変更）の同意で表示する文書か。 */
    public boolean appliesTo(String consentType) {
        return Codes.DOC_TARGET_COMMON.equals(targetType) || targetType.equals(consentType);
    }
}
