package com.driverline.planning.department;

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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class DepartmentRepositoryIT {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findsDepartmentByCode() {
        entityManager.persistAndFlush(new Department("OPS", "Operations"));
        entityManager.clear();

        assertThat(departmentRepository.findByCode("OPS"))
                .get()
                .extracting(Department::getName)
                .isEqualTo("Operations");
    }

    @Test
    void rejectsDuplicateDepartmentCode() {
        departmentRepository.saveAndFlush(new Department("OPS", "Operations"));

        assertThatThrownBy(() -> departmentRepository.saveAndFlush(new Department("OPS", "Duplicate")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
