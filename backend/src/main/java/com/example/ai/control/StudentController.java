package com.example.ai.control;

import com.example.ai.entity.Student;
import com.example.ai.pojo.LeeResult;
import com.example.ai.service.StudentService;
import com.example.ai.security.Role;
import com.example.ai.security.RoleRequired;
import com.example.ai.utils.OperatorResolver;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @RoleRequired({Role.ADMIN, Role.TEACHER})
    @PostMapping
    public LeeResult<Student> create(HttpServletRequest request,
                                     @RequestBody Student student) {
        return LeeResult.ok(studentService.create(student, OperatorResolver.resolve(request)));
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER, Role.STUDENT})
    @GetMapping
    public LeeResult<List<Student>> list() {
        return LeeResult.ok(studentService.list());
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER, Role.STUDENT})
    @GetMapping("/{id}")
    public LeeResult<Student> get(@PathVariable Integer id) {
        return studentService.get(id)
                .map(LeeResult::ok)
                .orElseGet(() -> LeeResult.fail("Student not found"));
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER})
    @PutMapping("/{id}")
    public LeeResult<Student> update(HttpServletRequest request,
                                     @PathVariable Integer id,
                                     @RequestBody Student student) {
        return studentService.update(id, student, OperatorResolver.resolve(request))
                .map(LeeResult::ok)
                .orElseGet(() -> LeeResult.fail("Student not found"));
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER})
    @DeleteMapping("/{id}")
    public LeeResult<Void> delete(HttpServletRequest request,
                                  @PathVariable Integer id) {
        boolean deleted = studentService.delete(id, OperatorResolver.resolve(request));
        return deleted ? LeeResult.ok() : LeeResult.fail("Student not found");
    }
}
