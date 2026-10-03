package com.example.appmgmt.web.view;

/**
 * メニュー画面（申込受付会社の申込メニュー、申込者メニュー）のタイル。
 * href があればリンク、formAction があれば POST フォームとして描画する。enabled=false は表示だけして押せない。
 */
public class MenuTile {
    private final String key;
    private final String title;
    private final String description;
    private String href;
    private String formAction;
    private String confirm;
    private boolean enabled = true;
    private String note;
    private String style = "primary";
    private boolean reasonInput;
    private String buttonLabel;

    public MenuTile(String key, String title, String description) {
        this.key = key;
        this.title = title;
        this.description = description;
    }

    public MenuTile link(String href) { this.href = href; return this; }
    public MenuTile post(String formAction) { this.formAction = formAction; return this; }
    public MenuTile confirm(String confirm) { this.confirm = confirm; return this; }
    public MenuTile disabled(String note) { this.enabled = false; this.note = note; return this; }
    public MenuTile style(String style) { this.style = style; return this; }
    public MenuTile withReasonInput() { this.reasonInput = true; return this; }
    /** ボタンの文言をタイトルと変える（タイルの位置・タイトルは固定のまま）。 */
    public MenuTile buttonLabel(String buttonLabel) { this.buttonLabel = buttonLabel; return this; }

    public String getKey() { return key; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getHref() { return href; }
    public String getFormAction() { return formAction; }
    public String getConfirm() { return confirm; }
    public boolean isEnabled() { return enabled; }
    public String getNote() { return note; }
    public String getStyle() { return style; }
    public boolean isReasonInput() { return reasonInput; }
    public boolean isPost() { return formAction != null; }
    public String getButtonLabel() { return buttonLabel == null ? title : buttonLabel; }
}
