package com.mri.exam.mapper;

import org.apache.ibatis.annotations.Select;

public interface PatientExamAccessMapper {
    @Select("""
            SELECT id
            FROM patient
            WHERE account_username = #{username}
              AND deleted = 0
            LIMIT 1
            """)
    Long findPatientId(String username);
}
