package com.mri.report.mapper;

import com.mri.report.entity.ReportEntity;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PatientReportAccessMapper {
    @Select("""
            SELECT r.*
            FROM mri_report r
            JOIN mri_exam_order e ON e.id = r.exam_order_id AND e.deleted = 0
            JOIN patient p ON p.id = e.patient_id AND p.deleted = 0
            WHERE p.account_username = #{username}
              AND r.deleted = 0
            ORDER BY r.id DESC
            """)
    List<ReportEntity> findReportsByUsername(String username);
}
