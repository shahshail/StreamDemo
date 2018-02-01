package com.streamunlimited.streamsdkdemo.data;

public enum ClientViewEnum {
    eClientViewPlayScreen {
        @Override
        public String toString() {
            return "PLAYSCREEN";
        }
    },
    eClientViewPlayWidget {
        @Override
        public String toString() {
            return "PLAYWIDGET";
        }
    },
    eClientViewBrowse {
        @Override
        public String toString() {
            return "BROWSE";
        }
    }
}
