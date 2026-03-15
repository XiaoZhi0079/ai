/*鍒涘缓鐢ㄦ埛琛?/
CREATE TABLE users (
                       id INT PRIMARY KEY AUTO_INCREMENT COMMENT '鐢ㄦ埛鍞竴ID',
                       username VARCHAR(50) NOT NULL UNIQUE COMMENT '鐢ㄦ埛鍚嶏紙鐧诲綍璐﹀彿锛?,
                       password VARCHAR(100) NOT NULL COMMENT '鍔犵洂鍝堝笇鍚庣殑瀵嗙爜',
                       role ENUM('ADMIN', 'TEACHER', 'STUDENT') NOT NULL COMMENT '瑙掕壊',
                       email VARCHAR(50) UNIQUE COMMENT '閭锛堢敤浜庢壘鍥炲瘑鐮侊級',
                       status TINYINT DEFAULT 1 COMMENT '鐘舵€侊紙1-姝ｅ父锛?-绂佺敤锛?,
                       created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
                       updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿'
/*                       last_login_time DATETIME COMMENT '鏈€鍚庣櫥褰曟椂闂?*/
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='閫氱敤鐢ㄦ埛淇℃伅琛?;

/*鏁欏笀涓撳睘淇℃伅琛紙鍏宠仈users.id锛?/
CREATE TABLE teachers (
                          id INT PRIMARY KEY AUTO_INCREMENT COMMENT '鏁欏笀淇℃伅ID',
                          user_id INT NOT NULL UNIQUE COMMENT '鍏宠仈閫氱敤鐢ㄦ埛琛?ID',
                          name VARCHAR(50) NOT NULL COMMENT '姓名',
                          department VARCHAR(50) COMMENT '鎵€灞為櫌绯伙紙濡傝绠楁満瀛﹂櫌锛?,
                          title VARCHAR(50) COMMENT '鑱岀О锛堝鏁欐巿/鍓暀鎺?璁插笀锛?,
                          research_field TEXT COMMENT '鐮旂┒棰嗗煙锛堟敮鎸丒moji锛?,
                          office_address VARCHAR(50) COMMENT '鍔炲叕瀹ゅ湴鍧€',
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿'
    /*     KEY idx_user_id (user_id) -- 鍔犵储寮曟彁鍗囧叧鑱旀煡璇㈡€ц兘锛堟浛浠ｅ閿殑鏍稿績锛?/
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鏁欏笀涓撳睘淇℃伅琛?;

/*瀛︾敓涓撳睘淇℃伅琛紙鍏宠仈users.id锛?/
CREATE TABLE students (
                          id INT PRIMARY KEY AUTO_INCREMENT COMMENT '瀛︾敓淇℃伅ID',
                          user_id INT NOT NULL UNIQUE COMMENT '鍏宠仈閫氱敤鐢ㄦ埛琛?ID',
                          name VARCHAR(50) NOT NULL COMMENT '姓名',
                          grade YEAR COMMENT '骞寸骇锛堝2023绾э級',
                          major VARCHAR(20) COMMENT '涓撲笟锛堝浜哄伐鏅鸿兘锛?,
                          class_name VARCHAR(50) COMMENT '鐝骇锛堝璁＄2301锛?,
                          dormitory VARCHAR(50) COMMENT '瀹胯垗鍙凤紙濡?鍙锋ゼ302锛?,
                          guardian_phone VARCHAR(20) COMMENT '鐩戞姢浜虹數璇?,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿'
/*                          KEY idx_user_id (user_id) -- 鍔犵储寮曟彁鍗囧叧鑱旀煡璇㈡€ц兘*/
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='瀛︾敓涓撳睘淇℃伅琛?;

/*鍒涘缓浼氳瘽淇℃伅琛紙鍏宠仈users.id锛?/
CREATE TABLE conversations (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '浼氳瘽ID',
                               user_id INT NOT NULL COMMENT '鐢ㄦ埛ID锛堝叧鑱攗sers.id锛?,
                               title VARCHAR(100) COMMENT '浼氳瘽鏍囬锛堣嚜鍔ㄧ敓鎴愭垨鐢ㄦ埛鑷畾涔夛級',
                               created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
                               updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浼氱敾淇℃伅琛?;

/*鍒涘缓浼氳瘽缁嗚妭琛紙鍏宠仈conversations.id锛?/
/*娉細浼氳瘽椤哄簭鏄惁闇€瑕佹敞鏄?/
CREATE TABLE messages (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '娑堟伅ID',
                          conversation_id BIGINT NOT NULL COMMENT '鍏宠仈浼氳瘽ID锛堝叧鑱攃onversations.id锛?,
                          sender ENUM('USER', 'AI') NOT NULL COMMENT '鍙戦€佽€?,
    /*                 content_type ENUM('TEXT', 'IMAGE', 'MIXED') NOT NULL COMMENT '鍐呭绫诲瀷',*/
                          content TEXT COMMENT '鏂囨湰鍐呭锛堟枃鏈秷鎭椂闈炵┖锛?,
                          image_url VARCHAR(500) COMMENT '鍥剧墖URL锛堟湁鍥剧墖鏃堕潪绌猴級',
                          sequence INT NOT NULL COMMENT '娑堟伅鍦ㄤ細璇濅腑鐨勯『搴忥紙鐢ㄤ簬杩樺師涓婁笅鏂囷級',
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍙戦€佹椂闂?
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浼氱敾缁嗚妭琛?;
/*RAG淇℃伅琛?/

/*瀛︾敓淇℃伅*/


# 璇剧▼淇℃伅锛堝叧鑱攖eachers.id锛?
CREATE TABLE courses (
                         id INT PRIMARY KEY AUTO_INCREMENT COMMENT '璇剧▼ID',
                         course_name VARCHAR(50) NOT NULL COMMENT '璇剧▼鍚嶇О',
                         teacher_id INT COMMENT '鎺堣鏁欏笀ID锛堝叧鑱旀暀甯堣〃.ID锛?,
                         credit DECIMAL(3,1) COMMENT '瀛﹀垎',
                         begin_date date NOT NULL COMMENT '寮€璇炬椂闂?,
                         end_date date NOT NULL COMMENT '缁撹鏃堕棿',
                         schedule TEXT COMMENT '璇剧▼瀹夋帓锛堝鈥滃懆涓€3-4鑺傦紝鏁?01鈥濓級',
                         description TEXT COMMENT '璇剧▼鎻忚堪',
                         created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
                         updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿'
);

/*鎴愮哗淇℃伅锛堝叧鑱攃ourses.id,students.id锛?/
CREATE TABLE grades (
                        id INT PRIMARY KEY AUTO_INCREMENT COMMENT '鎴愮哗ID',
                        student_id INT NOT NULL COMMENT '瀛︾敓ID锛堝叧鑱斿鐢熻〃.ID锛?,
                        course_id INT NOT NULL COMMENT '璇剧▼ID(鍏宠仈璇剧▼琛?ID)',
                        score DECIMAL(5,2) NOT NULL COMMENT '鎴愮哗鍒嗘暟',
                        semester YEAR NOT NULL COMMENT '瀛﹀勾',
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '褰曞叆鏃堕棿',
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鎴愮哗淇℃伅琛?;

/* operation_logs for CRUD audit */
CREATE TABLE operation_logs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'log id',
    operator VARCHAR(50) NOT NULL COMMENT 'operator',
    action VARCHAR(255) NOT NULL COMMENT 'action',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'created time'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='operation logs';




