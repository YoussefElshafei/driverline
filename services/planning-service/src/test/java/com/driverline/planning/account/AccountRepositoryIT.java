package com.driverline.planning.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.driverline.planning.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AccountRepositoryIT {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findsAccountByCodeWithItsType() {
        entityManager.persistAndFlush(new Account("4000", "Contract Cleaning Revenue", AccountType.REVENUE));
        entityManager.clear();

        Account found = accountRepository.findByCode("4000").orElseThrow();

        assertThat(found.getName()).isEqualTo("Contract Cleaning Revenue");
        assertThat(found.getType()).isEqualTo(AccountType.REVENUE);
    }

    @Test
    void returnsEmptyForUnknownCode() {
        assertThat(accountRepository.findByCode("9999")).isEmpty();
    }

    @Test
    void rejectsDuplicateAccountCode() {
        accountRepository.saveAndFlush(new Account("5300", "Vehicle & Fuel", AccountType.EXPENSE));

        assertThatThrownBy(() -> accountRepository.saveAndFlush(new Account("5300", "Duplicate", AccountType.EXPENSE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsUnknownAccountTypeEvenWhenJavaIsBypassed() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                        "INSERT INTO account (code, name, type) VALUES ('7000', 'Bad', 'ASSET')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
