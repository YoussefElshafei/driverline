package com.driverline.planning.reference;

import com.driverline.planning.account.Account;
import com.driverline.planning.account.AccountType;

public record AccountResponse(String code, String name, AccountType type) {

    static AccountResponse from(Account account) {
        return new AccountResponse(account.getCode(), account.getName(), account.getType());
    }
}
