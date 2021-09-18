package com.cst438.domain;

public class StudentDTO {

	public int student_id;
	public String name;
	public String email;
	public int statusCode;
	public String status;
	

	public int getStudent_id() {
		return student_id;
	}
	public void setStudent_id(int student_id) {
		this.student_id = student_id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public int getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	@Override
	public String toString() {
		return "StudentDTO [student_id=" + student_id + ", name=" + name + ", email=" + email + ", statusCode="
				+ statusCode + ", status=" + status + "]";
	}
	
}
