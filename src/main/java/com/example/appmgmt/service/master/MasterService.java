package com.example.appmgmt.service.master;

import com.example.appmgmt.common.BusinessException;
import com.example.appmgmt.common.OptimisticLockException;
import com.example.appmgmt.common.PasswordHasher;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.dao.ApprovalRouteDao;
import com.example.appmgmt.dao.CompanyDivDao;
import com.example.appmgmt.dao.EmployeeDao;
import com.example.appmgmt.domain.ApprovalRoute;
import com.example.appmgmt.domain.ApprovalRouteStep;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.CompanyDiv;
import com.example.appmgmt.domain.Employee;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** F15 マスタメンテナンス（社員・会社区分・承認ルート）。 */
public class MasterService {

    private final EmployeeDao employeeDao;
    private final CompanyDivDao companyDivDao;
    private final ApprovalRouteDao routeDao;

    public MasterService(EmployeeDao employeeDao, CompanyDivDao companyDivDao, ApprovalRouteDao routeDao) {
        this.employeeDao = employeeDao;
        this.companyDivDao = companyDivDao;
        this.routeDao = routeDao;
    }

    public List<Employee> employees() {
        return Tx.execute(employeeDao::findAll);
    }

    public Optional<Employee> employee(long id) {
        return Tx.execute(conn -> employeeDao.findById(conn, id));
    }

    /** 登録（employeeId = 0）または更新。更新時にパスワードが空なら変更しない。 */
    public long saveEmployee(Employee e, String rawPassword, int expectedRowVersion) {
        return Tx.execute(conn -> {
            if (e.getEmployeeId() == 0) {
                if (employeeDao.findByNo(conn, e.getEmployeeNo()).isPresent()) {
                    throw new BusinessException("E006", "社員番号");
                }
                if (rawPassword == null || rawPassword.isEmpty()) {
                    throw new BusinessException("E001", "パスワード");
                }
                e.setPasswordHash(PasswordHasher.hash(rawPassword));
                return employeeDao.insert(conn, e);
            }
            Employee current = employeeDao.findById(conn, e.getEmployeeId()).orElseThrow(OptimisticLockException::new);
            e.setPasswordHash(rawPassword == null || rawPassword.isEmpty() ? current.getPasswordHash() : PasswordHasher.hash(rawPassword));
            if (employeeDao.update(conn, e, expectedRowVersion) != 1) {
                throw new OptimisticLockException();
            }
            return e.getEmployeeId();
        });
    }

    public void toggleEmployeeValid(long id, int expectedRowVersion, long loginEmployeeId) {
        Tx.executeVoid(conn -> {
            Employee e = employeeDao.findById(conn, id).orElseThrow(OptimisticLockException::new);
            if (e.getEmployeeId() == loginEmployeeId) {
                throw new BusinessException("E103");
            }
            e.setValidFlg(e.isValid() ? Codes.FLG_OFF : Codes.FLG_ON);
            if (employeeDao.update(conn, e, expectedRowVersion) != 1) {
                throw new OptimisticLockException();
            }
        });
    }

    public List<CompanyDiv> companyDivs() {
        return Tx.execute(companyDivDao::findAll);
    }

    public void saveCompanyDivs(List<CompanyDiv> divs) {
        Tx.executeVoid(conn -> {
            for (CompanyDiv d : divs) {
                if (companyDivDao.update(conn, d, d.getRowVersion()) != 1) {
                    throw new OptimisticLockException();
                }
            }
        });
    }

    public List<ApprovalRoute> routes() {
        return Tx.execute(routeDao::findAll);
    }

    public Optional<ApprovalRoute> route(long id) {
        return Tx.execute(conn -> routeDao.findById(conn, id));
    }

    public List<Employee> approverCandidates(String companyDiv) {
        return Tx.execute(conn -> employeeDao.findApproverCandidates(conn, companyDiv));
    }

    public long saveRoute(ApprovalRoute r, int expectedRowVersion) {
        return Tx.execute(conn -> {
            Set<Long> seen = new HashSet<>();
            if (r.getSteps().isEmpty()) {
                throw new BusinessException("E001", "承認者");
            }
            for (ApprovalRouteStep s : r.getSteps()) {
                Employee e = employeeDao.findById(conn, s.getApproverEmployeeId()).orElseThrow(() -> new BusinessException("E108"));
                if (!e.isValid() || !Codes.ROLE_APPROVER.equals(e.getRoleCd()) || !e.getCompanyDiv().equals(r.getCompanyDiv()) || !seen.add(s.getApproverEmployeeId())) {
                    throw new BusinessException("E108");
                }
            }
            if (r.getRouteId() == 0) {
                return routeDao.insert(conn, r);
            }
            if (routeDao.update(conn, r, expectedRowVersion) != 1) {
                throw new OptimisticLockException();
            }
            return r.getRouteId();
        });
    }

    public void deleteRoute(long id) {
        Tx.executeVoid(conn -> routeDao.delete(conn, id));
    }
}
