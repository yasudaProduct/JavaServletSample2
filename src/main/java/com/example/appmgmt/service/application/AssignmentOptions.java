package com.example.appmgmt.service.application;

import com.example.appmgmt.domain.CompanyDiv;
import com.example.appmgmt.domain.Department;
import com.example.appmgmt.domain.Employee;
import java.util.List;

/** 申込入力（SC04）の担当（会社 > 部署 > 担当者）の選択肢。部署は有効なもの、担当者は有効な担当者権限（01）の社員。 */
public class AssignmentOptions {
    private final List<CompanyDiv> companies;
    private final List<Department> departments;
    private final List<Employee> owners;

    public AssignmentOptions(List<CompanyDiv> companies, List<Department> departments, List<Employee> owners) {
        this.companies = companies;
        this.departments = departments;
        this.owners = owners;
    }

    public List<CompanyDiv> getCompanies() { return companies; }
    public List<Department> getDepartments() { return departments; }
    public List<Employee> getOwners() { return owners; }
}
