package com.example.ai.control;

import com.example.ai.entity.Teacher;
import com.example.ai.pojo.LeeResult;
import com.example.ai.service.TeacherService;
import com.example.ai.security.Role;
import com.example.ai.security.RoleRequired;
import com.example.ai.utils.OperatorResolver;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @RoleRequired({Role.ADMIN})
    @PostMapping
    public LeeResult<Teacher> create(HttpServletRequest request,
                                     @RequestBody Teacher teacher,
                                     @RequestHeader(value = "X-User", required = false) String operator,
                                     @RequestHeader(value = "X-Role", required = false) String role) {
        try {
            return LeeResult.ok(teacherService.create(teacher, OperatorResolver.resolve(request, operator, role)));
        } catch (IllegalArgumentException ex) {
            return LeeResult.fail(ex.getMessage());
        }
    }

    @RoleRequired({Role.ADMIN})
    @GetMapping
    public LeeResult<List<Teacher>> list() {
        return LeeResult.ok(teacherService.list());
    }

    @RoleRequired({Role.ADMIN})
    @GetMapping("/{id}")
    public LeeResult<Teacher> get(@PathVariable Integer id) {
        return teacherService.get(id)
                .map(LeeResult::ok)
                .orElseGet(() -> LeeResult.fail("Teacher not found"));
    }

    @RoleRequired({Role.ADMIN})
    @PutMapping("/{id}")
    public LeeResult<Teacher> update(HttpServletRequest request,
                                     @PathVariable Integer id,
                                     @RequestBody Teacher teacher,
                                     @RequestHeader(value = "X-User", required = false) String operator,
                                     @RequestHeader(value = "X-Role", required = false) String role) {
        try {
            return teacherService.update(id, teacher, OperatorResolver.resolve(request, operator, role))
                    .map(LeeResult::ok)
                    .orElseGet(() -> LeeResult.fail("Teacher not found"));
        } catch (IllegalArgumentException ex) {
            return LeeResult.fail(ex.getMessage());
        }
    }

    @RoleRequired({Role.ADMIN})
    @DeleteMapping("/{id}")
    public LeeResult<Void> delete(HttpServletRequest request,
                                  @PathVariable Integer id,
                                  @RequestHeader(value = "X-User", required = false) String operator,
                                  @RequestHeader(value = "X-Role", required = false) String role) {
        boolean deleted = teacherService.delete(id, OperatorResolver.resolve(request, operator, role));
        return deleted ? LeeResult.ok() : LeeResult.fail("Teacher not found");
    }
}
