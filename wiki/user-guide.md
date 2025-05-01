# 사용자 가이드

이 가이드는 LukeVanilla 메시징 시스템의 사용 방법을 설명합니다.

## 목차

- [명령어](#명령어)
- [메시지 형식](#메시지-형식)
- [사용 시나리오](#사용-시나리오)
- [권한 관리](#권한-관리)
- [팁과 요령](#팁과-요령)

## 명령어

### Velocity 프록시 명령어

프록시 서버에서 사용할 수 있는 명령어입니다.

#### `/sendmessage <서버명> <메시지>`

특정 백엔드 서버로 메시지를 전송합니다.

- **필수 매개변수**:
  - `<서버명>`: 대상 서버 이름 (velocity.toml에 구성된 서버 중 하나)
  - `<메시지>`: 전송할 메시지

- **예시**:
  ```
  /sendmessage survival 서버를 5분 후에 재시작합니다
  ```

- **단축 명령어**: `/smsg <서버명> <메시지>`

- **권한**: `lukevanilla.command.sendmessage`

- **응답**:
  - 성공: ✅ 'survival' 서버로 메시지를 전송했습니다: 서버를 5분 후에 재시작합니다
  - 실패: ❌ 메시지 전송에 실패했습니다.

### Paper 서버 명령어

백엔드 서버에서 사용할 수 있는 명령어입니다.

#### `/sendmessage <서버명> <메시지>`

다른 백엔드 서버로 메시지를 전송합니다.

- **필수 매개변수**:
  - `<서버명>`: 대상 서버 이름 (config.yml의 servers 목록 중 하나)
  - `<메시지>`: 전송할 메시지

- **예시**:
  ```
  /sendmessage creative 비상: 크리에이티브 서버에 불 확산 문제가 있습니다
  ```

- **단축 명령어**: `/smsg <서버명> <메시지>`

- **권한**: `lukevanilla.command.sendmessage`

- **응답**:
  - 성공: ✅ 'creative' 서버로 메시지를 전송했습니다: 비상: 크리에이티브 서버에 불 확산 문제가 있습니다
  - 실패: ❌ 메시지 전송에 실패했습니다.

## 메시지 형식

메시지 내용은 일반 텍스트이며 다음 제한 사항이 있습니다:

- **최대 길이**: 1024자
- **지원 문자**: 모든 Unicode 문자 지원 (다국어 가능)
- **형식 지정**: 기본 채팅 색상 코드 지원 (`&a`, `&b` 등)

### 색상 코드 사용 예시

메시지에 색상 코드를 사용하여 강조할 수 있습니다:

```
/sendmessage survival &c경고: &e서버를 1분 후에 재시작합니다!
```

결과:
- <span style="color:red">경고:</span> <span style="color:yellow">서버를 1분 후에 재시작합니다!</span>

## 사용 시나리오

### 1. 서버 간 공지 사항

관리자가 모든 서버에 동시에 공지 사항을 전송할 수 있습니다.

1. 서버 목록을 확인합니다:
   ```
   /server
   ```

2. 각 서버에 메시지를 전송합니다:
   ```
   /sendmessage lobby &a[공지] &f서버 점검이 30분 후에 시작됩니다
   /sendmessage survival &a[공지] &f서버 점검이 30분 후에 시작됩니다
   /sendmessage creative &a[공지] &f서버 점검이 30분 후에 시작됩니다
   ```

### 2. 특정 서버 재시작 공지

특정 서버에만 재시작 알림을 전송할 수 있습니다:

```
/sendmessage survival &c[시스템] &e서버가 60초 후에 재시작됩니다. 건축물을 저장해주세요!
```

### 3. 서버 간 개인 메시지

서버 A에 있는 관리자가 서버 B에 있는 플레이어에게 메시지를 전송할 수 있습니다.

```
/sendmessage creative &9[스태프] &fPlayer1: 도움이 필요하신가요?
```

### 4. 서버 간 이벤트 동기화

여러 서버에서 동시에 이벤트를 시작할 수 있습니다:

```
/sendmessage survival &d[이벤트] &f더블 경험치 이벤트가 시작되었습니다!
/sendmessage creative &d[이벤트] &f더블 경험치 이벤트가 시작되었습니다!
```

### 5. 서버 상태 모니터링

한 서버에서 다른 서버에 상태 정보를 전송할 수 있습니다:

```
/sendmessage lobby &7[상태] &f서바이벌 서버 TPS: 19.8, 메모리: 3.2GB/4GB
```

## 권한 관리

### Velocity 권한

- `lukevanilla.command.sendmessage` - `/sendmessage` 및 `/smsg` 명령어 사용 권한

### Paper 권한

- `lukevanilla.command.sendmessage` - `/sendmessage` 및 `/smsg` 명령어 사용 권한

### LuckPerms를 사용한 권한 설정 예시

```
# 관리자 그룹에 권한 부여
/lp group admin permission set lukevanilla.command.sendmessage true

# 특정 플레이어에게 권한 부여
/lp user Steve permission set lukevanilla.command.sendmessage true
```

## 팁과 요령

### 효율적인 메시지 전송

1. **서버 이름 자동 완성 사용**: 탭 키를 눌러 서버 이름을 자동으로 완성할 수 있습니다.

2. **단축 명령어 활용**: `/smsg`는 `/sendmessage`의 단축 버전입니다.

3. **메시지 형식 일관성 유지**: 일관된 메시지 형식(예: `[카테고리] 메시지`)을 사용하면 메시지를 쉽게 식별할 수 있습니다.

### 서버 이름 별칭 사용

서버 이름이 길거나 복잡한 경우, Paper 플러그인 설정에서 별칭을 정의할 수 있습니다:

```yaml
# config.yml

servers:
  - "lobby"
  - "survival"
  - "creative"
  - "minigames"
  
server-aliases:
  "s": "survival"
  "c": "creative"
  "l": "lobby"
  "mg": "minigames"
```

이렇게 설정하면 `/sendmessage s 안녕하세요`와 같이 단축된 서버 이름을 사용할 수 있습니다.

### 주기적인 메시지 설정

다른 스케줄링 플러그인과 함께 사용하여 주기적인 메시지를 설정할 수 있습니다.

예를 들어, CMI 또는 EssentialsX와 같은 플러그인을 사용하여 매 시간마다 특정 서버에 메시지를 전송할 수 있습니다:

```
/cmi schedule add hourlyMessage 1h -cmd:sendmessage survival &a[알림] &f서버가 원활하게 운영 중입니다.
```

### 플레이어 필터링 활용

서버에서 메시지를 수신할 때 특정 권한을 가진 플레이어에게만 표시하도록 설정할 수 있습니다. 이는 개발자가 API를 통해 구현할 수 있습니다:

```java
@EventHandler
public void onMessageReceived(MessageReceivedEvent event) {
    String message = event.getMessage();
    
    if (message.startsWith("[스태프]")) {
        // 스태프 권한이 있는 플레이어에게만 메시지 표시
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("staff.messages")) {
                player.sendMessage(message);
            }
        }
    } else {
        // 일반 메시지는 모든 플레이어에게 표시
        Bukkit.broadcastMessage(message);
    }
}
``` 