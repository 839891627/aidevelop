package com.example.aidevelop.model.dto.loanform;

import java.util.Map;

/**
 * 贷款表单所有字段的完整数据结构
 */
public class LoanFormData {
    // 基本信息
    private String applicantName;
    private String idNumber;
    private String phone;
    private String address;
    private String occupation;
    private String income;

    // 贷款信息
    private String loanType;
    private String loanAmount;
    private String loanTerm;
    private String loanPurpose;
    private String repaymentSource;

    // 担保信息
    private String guaranteeType;
    private String collateral;
    private String collateralValue;
    private String guarantorName;
    private String guarantorPhone;

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public String getIncome() { return income; }
    public void setIncome(String income) { this.income = income; }

    public String getLoanType() { return loanType; }
    public void setLoanType(String loanType) { this.loanType = loanType; }

    public String getLoanAmount() { return loanAmount; }
    public void setLoanAmount(String loanAmount) { this.loanAmount = loanAmount; }

    public String getLoanTerm() { return loanTerm; }
    public void setLoanTerm(String loanTerm) { this.loanTerm = loanTerm; }

    public String getLoanPurpose() { return loanPurpose; }
    public void setLoanPurpose(String loanPurpose) { this.loanPurpose = loanPurpose; }

    public String getRepaymentSource() { return repaymentSource; }
    public void setRepaymentSource(String repaymentSource) { this.repaymentSource = repaymentSource; }

    public String getGuaranteeType() { return guaranteeType; }
    public void setGuaranteeType(String guaranteeType) { this.guaranteeType = guaranteeType; }

    public String getCollateral() { return collateral; }
    public void setCollateral(String collateral) { this.collateral = collateral; }

    public String getCollateralValue() { return collateralValue; }
    public void setCollateralValue(String collateralValue) { this.collateralValue = collateralValue; }

    public String getGuarantorName() { return guarantorName; }
    public void setGuarantorName(String guarantorName) { this.guarantorName = guarantorName; }

    public String getGuarantorPhone() { return guarantorPhone; }
    public void setGuarantorPhone(String guarantorPhone) { this.guarantorPhone = guarantorPhone; }
}
