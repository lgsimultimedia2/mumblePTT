package com.jio.jiotalkie.db;

import androidx.annotation.NonNull;

public class CertificateEntity {
    private final long certificateId;
    private final String certificateName;

    protected CertificateEntity(long certificateId, String certificateName) {
        this.certificateId = certificateId;
        this.certificateName = certificateName;
    }

    public long getCertificateId() {
        return certificateId;
    }

    public String getCertificateName() {
        return certificateName;
    }

    @NonNull
    @Override
    public String toString() {
        return certificateName;
    }
}
