package com.alfatahi.erp.dto;

import java.util.ArrayList;
import java.util.List;

public class UserPermissionsForm {
    private List<Row> pages = new ArrayList<>();
    public List<Row> getPages() { return pages; }
    public void setPages(List<Row> pages) { this.pages = pages; }

    public static class Row {
        private String pageKey;
        private String mode = "default";
        private boolean view;
        private boolean edit;
        private boolean delete;
        public String getPageKey() { return pageKey; }
        public void setPageKey(String pageKey) { this.pageKey = pageKey; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public boolean isView() { return view; }
        public void setView(boolean view) { this.view = view; }
        public boolean isEdit() { return edit; }
        public void setEdit(boolean edit) { this.edit = edit; }
        public boolean isDelete() { return delete; }
        public void setDelete(boolean delete) { this.delete = delete; }
    }
}
