package com.cst438.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cst438.domain.Course;
import com.cst438.domain.CourseRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.ScheduleDTO;
import com.cst438.domain.Student;
import com.cst438.domain.StudentDTO;
import com.cst438.domain.StudentRepository;
import com.cst438.service.GradebookService;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class ScheduleController {

	@Autowired
	CourseRepository courseRepository;

	@Autowired
	StudentRepository studentRepository;

	@Autowired
	EnrollmentRepository enrollmentRepository;

	@Autowired
	GradebookService gradebookService;

	// gets current schedule of student
	@GetMapping("/schedule") // http://server/schedule?year=nnnn&semester=xxxx
	public ScheduleDTO getSchedule(@RequestParam("year") int year, @RequestParam("semester") String semester) {

		String student_email = "test@csumb.edu"; // student's email

		Student student = studentRepository.findByEmail(student_email);
		if (student != null) {
			List<Enrollment> enrollments = enrollmentRepository.findStudentSchedule(student_email, year, semester);
			ScheduleDTO sched = createSchedule(year, semester, student, enrollments);
			return sched;
		} else {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student not found. ");
		}
	}

	@PostMapping("/schedule")
	@Transactional
	public ScheduleDTO.CourseDTO addCourse(@RequestBody ScheduleDTO.CourseDTO courseDTO) {

		String student_email = "test@csumb.edu"; // student's email

		Student student = studentRepository.findByEmail(student_email);
		Course course = courseRepository.findByCourse_id(courseDTO.course_id);

		// student.status
		// = 0 ok to register
		// != 0 hold on registration. student.status may have reason for hold.

		if (student != null && course != null && student.getStatusCode() == 0) {
			// TODO check that today's date is not past add deadline for the course.
			Enrollment enrollment = new Enrollment();
			enrollment.setStudent(student);
			enrollment.setCourse(course);
			enrollment.setYear(course.getYear());
			enrollment.setSemester(course.getSemester());
			Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

			gradebookService.enrollStudent(student_email, student.getName(), course.getCourse_id());

			ScheduleDTO.CourseDTO result = createCourseDTO(savedEnrollment);
			return result;
		} else {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Course_id invalid or student not allowed to register for the course.  " + courseDTO.course_id);
		}

	}

	// @Author Kristelle Dimapilis
	// method to add a new student into system
	@PostMapping("/student")
	@Transactional
	public StudentDTO addStudent(@RequestBody StudentDTO student) { // example input: {"name": "haley", "email":
																	// "haley@csumb.edu"}

		String student_name = student.getName(); // student's name
		String student_email = student.getEmail(); // student's email

		Student findStudent = studentRepository.findByEmail(student_email);

		if (findStudent == null) { // if student is not found, create and add new student

			Student s = new Student();
			s.setEmail(student_email);
			s.setName(student_name);
			s = studentRepository.save(s);

			StudentDTO result = createStudentDTO(s);

			return result;
		} else { // if student is found, throw exception

			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student record already exists.");
		}

	}

	@PutMapping("/student/add-hold")
	@Transactional
	public void addHold(@RequestParam String student_email) {

		Student student = studentRepository.findByEmail(student_email);

		if (student != null && student.getStatusCode() == 0) {
			student.setStatusCode(1);
			studentRepository.save(student);
		} else {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Student record does not exist or is already placed on hold.");
		}
	}

	@PutMapping("/student/drop-hold")
	@Transactional
	public void dropHold(@RequestParam String student_email) {

		Student student = studentRepository.findByEmail(student_email);

		if (student != null && student.getStatusCode() == 1) {
			student.setStatusCode(0);
			studentRepository.save(student);
		} else {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Student record does not exist or is not on hold.");
		}
	}

	@DeleteMapping("/schedule/{enrollment_id}")
	@Transactional
	public void dropCourse(@PathVariable int enrollment_id) {

		String student_email = "test@csumb.edu"; // student's email

		// TODO check that today's date is not past deadline to drop course.

		Enrollment enrollment = enrollmentRepository.findById(enrollment_id);

		// verify that student is enrolled in the course.
		if (enrollment != null && enrollment.getStudent().getEmail().equals(student_email)) {
			// OK. drop the course.
			enrollmentRepository.delete(enrollment);
		} else {
			// something is not right with the enrollment.
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enrollment_id invalid. " + enrollment_id);
		}
	}

	/*
	 * helper method to transform course, enrollment, student entities into a an
	 * instance of ScheduleDTO to return to front end. This makes the front end less
	 * dependent on the details of the database.
	 */
	private ScheduleDTO createSchedule(int year, String semester, Student s, List<Enrollment> enrollments) {
		ScheduleDTO result = new ScheduleDTO();
		result.semester = semester;
		result.year = year;
		result.student_email = s.getEmail();
		result.student_id = s.getStudent_id();
		ArrayList<ScheduleDTO.CourseDTO> courses = new ArrayList<>();

		for (Enrollment e : enrollments) {
			ScheduleDTO.CourseDTO courseDTO = createCourseDTO(e);
			courses.add(courseDTO);
		}
		result.courses = courses;
		return result;
	}

	private ScheduleDTO.CourseDTO createCourseDTO(Enrollment e) {
		ScheduleDTO.CourseDTO courseDTO = new ScheduleDTO.CourseDTO();
		Course c = e.getCourse();
		courseDTO.id = e.getEnrollment_id();
		courseDTO.building = c.getBuilding();
		courseDTO.course_id = c.getCourse_id();
		courseDTO.endDate = c.getEnd().toString();
		courseDTO.instructor = c.getInstructor();
		courseDTO.room = c.getRoom();
		courseDTO.section = c.getSection();
		courseDTO.startDate = c.getStart().toString();
		courseDTO.times = c.getTimes();
		courseDTO.title = c.getTitle();
		courseDTO.grade = e.getCourseGrade();
		return courseDTO;
	}

	// @Author Kristelle Dimapilis
	private StudentDTO createStudentDTO(Student s) {
		StudentDTO studentDTO = new StudentDTO();
		studentDTO.student_id = s.getStudent_id();
		studentDTO.name = s.getName();
		studentDTO.email = s.getEmail();
		studentDTO.statusCode = s.getStatusCode();
		studentDTO.status = s.getStatus();
		return studentDTO;
	}

}
