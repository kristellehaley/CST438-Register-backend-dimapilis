package com.cst438.domain;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends CrudRepository <Student, Integer> {
	
	public Student findByEmail(@Param("email") String email);

}
