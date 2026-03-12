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
                                     @RequestBody Student student,
                                     @RequestHeader(value = "X-User", required = false) String operator,
                                     @RequestHeader(value = "X-Role", required = false) String role) {
        try {
            return LeeResult.ok(studentService.create(student, OperatorResolver.resolve(request, operator, role)));
        } catch (IllegalArgumentException ex) {
            return LeeResult.fail(ex.getMessage());
        }
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER})
    @GetMapping
    public LeeResult<List<Student>> list() {
        return LeeResult.ok(studentService.list());
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER})
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
                                     @RequestBody Student student,
                                     @RequestHeader(value = "X-User", required = false) String operator,
                                     @RequestHeader(value = "X-Role", required = false) String role) {
        try {
            return studentService.update(id, student, OperatorResolver.resolve(request, operator, role))
                    .map(LeeResult::ok)
                    .orElseGet(() -> LeeResult.fail("Student not found"));
        } catch (IllegalArgumentException ex) {
            return LeeResult.fail(ex.getMessage());
        }
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER})
    @DeleteMapping("/{id}")
    public LeeResult<Void> delete(HttpServletRequest request,
                                  @PathVariable Integer id,
                                  @RequestHeader(value = "X-User", required = false) String operator,
                                  @RequestHeader(value = "X-Role", required = false) String role) {
        boolean deleted = studentService.delete(id, OperatorResolver.resolve(request, operator, role));
        return deleted ? LeeResult.ok() : LeeResult.fail("Student not found");
    }
}
