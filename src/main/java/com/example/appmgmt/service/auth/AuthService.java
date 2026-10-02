package com.example.appmgmt.service.auth;

import com.example.appmgmt.common.PasswordHasher;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.dao.CompanyDivDao;
import com.example.appmgmt.dao.EmployeeDao;
import com.example.appmgmt.domain.CompanyDiv;
import com.example.appmgmt.domain.Employee;
import com.example.appmgmt.domain.LoginUser;
import java.util.Optional;

/** ログイン（10. 16 章）。社員番号とパスワードで認証する。ロック機能は未実装（99. No.30）。 */
public class AuthService {

    private final EmployeeDao employeeDao;
    private final CompanyDivDao companyDivDao;

    public AuthService(EmployeeDao employeeDao, CompanyDivDao companyDivDao) {
        this.employeeDao = employeeDao;
        this.companyDivDao = companyDivDao;
    }

    public Optional<LoginUser> login(String employeeNo, String password) {
        return Tx.execute(conn -> {
            Optional<Employee> e = employeeDao.findByNo(conn, employeeNo);
            if (e.isEmpty() || !e.get().isValid() || !PasswordHasher.verify(password, e.get().getPasswordHash())) {
                return Optional.empty();
            }
            String divName = companyDivDao.find(conn, e.get().getCompanyDiv()).map(CompanyDiv::getCompanyDivName).orElse("");
            return Optional.of(new LoginUser(e.get(), divName));
        });
    }
}
