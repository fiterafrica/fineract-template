package org.apache.fineract.portfolio.loanaccount.data;

import lombok.Data;

@Data
public class KivaLoanExceptions {

    private String loanId;
    private Throwable error;
}
