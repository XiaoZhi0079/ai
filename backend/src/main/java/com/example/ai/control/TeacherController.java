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
                                     @RequestBody Teacher teacher) {
        return LeeResult.ok(teacherService.create(teacher, OperatorResolver.resolve(request)));
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER, Role.STUDENT})
    @GetMapping
    public LeeResult<List<Teacher>> list() {
        return LeeResult.ok(teacherService.list());
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER, Role.STUDENT})
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
                                     @RequestBody Teacher teacher) {
        return teacherService.update(id, teacher, OperatorResolver.resolve(request))
                .map(LeeResult::ok)
                .orElseGet(() -> LeeResult.fail("Teacher not found"));
    }

    @RoleRequired({Role.ADMIN})
    @DeleteMapping("/{id}")
    public LeeResult<Void> delete(HttpServletRequest request,
                                  @PathVariable Integer id) {
        boolean deleted = teacherService.delete(id, OperatorResolver.resolve(request));
        return deleted ? LeeResult.ok() : LeeResult.fail("Teacher not found");
    }
}
