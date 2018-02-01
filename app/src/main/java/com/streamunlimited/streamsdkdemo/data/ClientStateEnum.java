package com.streamunlimited.streamsdkdemo.data;

public enum ClientStateEnum {
    eClientStatePlay {
        @Override
        public String toString() {
            return "PLAY";
        }
    },
    eClientStateBrowse {
        @Override
        public String toString() {
            return "BROWSE";
        }
    },
    eClientStateIdle {
        @Override
        public String toString() {
            return "IDLE";
        }
    }
}
