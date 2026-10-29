package com.mycompany.clinica.security;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Registro de un JWT invalidado antes de su expiración natural. */
@Document(collection = "tokens_revocados")
public class TokenRevocado {

    /** El jti es único por token y sirve como identificador del documento. */
    @Id
    private String jti;

    /** MongoDB elimina el registro una vez transcurrida la fecha de expiración. */
    @Indexed(expireAfterSeconds = 0)
    private Date expiraEn;

    public TokenRevocado() {
    }

    public TokenRevocado(String jti, Date expiraEn) {
        this.jti = jti;
        this.expiraEn = expiraEn;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Date getExpiraEn() {
        return expiraEn;
    }

    public void setExpiraEn(Date expiraEn) {
        this.expiraEn = expiraEn;
    }
}
