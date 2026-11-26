package com.pvz;

import com.pvz.model.base.Entity;
import com.pvz.model.board.GameBoard;
import com.pvz.model.board.TerrainQuery;
import com.pvz.model.tile.TileType;
import com.pvz.model.tile.WaterTile;
import com.pvz.model.wave.EscalatingRandomSpawnPolicy;
import com.pvz.model.wave.SpawnSpec;
import com.pvz.model.wave.Wave;
import com.pvz.model.wave.WaveManager;
import com.pvz.model.zombie.*;
import com.pvz.model.zombie.state.*;
import com.pvz.system.event.CombatEventBus;
import com.pvz.system.event.CombatListener;
import com.pvz.system.event.ZombieDeathEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ZombieFSMTest {

    private NormalZombie zombie;
    private GameBoard board;

    // A dummy plant entity for target interaction tests
    private static class DummyPlant extends Entity {
        public DummyPlant(double x, double y, int hp) {
            super(x, y, 40.0, 40.0, hp);
        }

        @Override
        public void update(double deltaTime) {}

        @Override
        public void render(javafx.scene.canvas.GraphicsContext gc) {}
    }

    @BeforeEach
    void setUp() {
        board = new GameBoard();
        zombie = new NormalZombie(500.0, 100.0, 0, board);
    }

    @Test
    @DisplayName("Kịch bản 1: NormalZombie khởi tạo ở WalkingState và di chuyển đúng x - speed * dt")
    void testScenario1_NormalZombieInitializationAndMovement() {
        assertEquals(50.0, zombie.getWidth(), 0.001);
        assertEquals(90.0, zombie.getHeight(), 0.001);
        assertEquals(200, zombie.getHp());
        assertEquals(17.0, zombie.getSpeed(), 0.001);
        assertEquals(25, zombie.getAttackDamage());
        assertFalse(zombie.isReadyToRemove());
        assertNotNull(zombie.getCurrentState());
        assertInstanceOf(WalkingState.class, zombie.getCurrentState());
        assertEquals("WALKING", zombie.getCurrentState().getStateName());

        double startX = zombie.getX();
        double dt = 0.5;
        zombie.update(dt);
        assertEquals(startX - 17.0 * dt, zombie.getX(), 0.001);
    }

    @Test
    @DisplayName("Kịch bản 2: WalkingState -> EatingState khi chạm mục tiêu sống, trừ máu mỗi giây, và quay lại WalkingState khi mục tiêu chết")
    void testScenario2_WalkingToEatingCycleAndDamage() {
        DummyPlant plant = new DummyPlant(490.0, 100.0, 50);
        zombie.onCollideWithTarget(plant, false);

        assertInstanceOf(EatingState.class, zombie.getCurrentState());
        assertEquals("EATING", zombie.getCurrentState().getStateName());
        assertEquals(plant, zombie.getCurrentTarget());

        // Sau 0.5s: chưa tới nhịp cắn (chu kỳ 1.0s)
        zombie.update(0.5);
        assertEquals(50, plant.getHp());

        // Thêm 0.5s (tổng 1.0s): cắn lần 1, trừ đúng 25 damage
        zombie.update(0.5);
        assertEquals(25, plant.getHp());
        assertTrue(plant.isAlive());
        assertInstanceOf(EatingState.class, zombie.getCurrentState());

        // Thêm 1.0s: cắn lần 2, cây chết -> chuyển lại về WalkingState
        zombie.update(1.0);
        assertEquals(0, plant.getHp());
        assertFalse(plant.isAlive());
        assertNull(zombie.getCurrentTarget());
        assertInstanceOf(WalkingState.class, zombie.getCurrentState());
    }

    @Test
    @DisplayName("Kịch bản 3: ConeheadZombie và BucketheadZombie hấp thụ sát thương vào armorHp trước khi trừ vào hp chính")
    void testScenario3_ArmorAbsorptionConeheadAndBuckethead() {
        // Kiểm tra ConeheadZombie (armorHp = 370, hp = 200)
        ConeheadZombie conehead = new ConeheadZombie(500.0, 100.0, 0, board);
        assertEquals(370, conehead.getArmorHp());
        assertEquals(200, conehead.getHp());

        conehead.takeDamage(200);
        assertEquals(170, conehead.getArmorHp());
        assertEquals(200, conehead.getHp());

        conehead.takeDamage(200); // 170 giáp vỡ + 30 xuyên vào hp chính
        assertEquals(0, conehead.getArmorHp());
        assertEquals(170, conehead.getHp());
        assertTrue(conehead.isAlive());

        // Kiểm tra BucketheadZombie (armorHp = 1100, hp = 200)
        BucketheadZombie buckethead = new BucketheadZombie(500.0, 100.0, 0, board);
        assertEquals(1100, buckethead.getArmorHp());
        assertEquals(200, buckethead.getHp());

        buckethead.takeDamage(1000);
        assertEquals(100, buckethead.getArmorHp());
        assertEquals(200, buckethead.getHp());

        buckethead.takeDamage(200); // 100 giáp vỡ + 100 xuyên vào hp chính
        assertEquals(0, buckethead.getArmorHp());
        assertEquals(100, buckethead.getHp());
        assertTrue(buckethead.isAlive());
    }

    @Test
    @DisplayName("Kịch bản 4: PoleVaultingZombie nhảy qua vật cản thường, giảm tốc độ, nhưng phải dừng ăn nếu gặp vật cản cao")
    void testScenario4_PoleVaultingMechanics() {
        PoleVaultingZombie pvz = new PoleVaultingZombie(500.0, 100.0, 0, board);
        assertEquals(31.0, pvz.getSpeed(), 0.001);
        assertEquals(500, pvz.getHp());
        assertFalse(pvz.hasVaulted());

        DummyPlant normalPlant = new DummyPlant(490.0, 100.0, 100);
        pvz.onCollideWithTarget(normalPlant, false);

        // Nhảy sào được kích hoạt
        assertInstanceOf(VaultingState.class, pvz.getCurrentState());
        assertEquals("VAULTING", pvz.getCurrentState().getStateName());

        // Hoàn tất nhảy (0.8s), tiến sang trái đúng CELL_WIDTH (80.0 px)
        pvz.update(0.8);
        assertTrue(pvz.hasVaulted());
        assertEquals(17.0, pvz.getSpeed(), 0.001);
        assertInstanceOf(WalkingState.class, pvz.getCurrentState());

        // Gặp cây thứ 2 sau khi đã nhảy: phải dừng lại ăn bình thường
        DummyPlant secondPlant = new DummyPlant(400.0, 100.0, 100);
        pvz.onCollideWithTarget(secondPlant, false);
        assertInstanceOf(EatingState.class, pvz.getCurrentState());

        // Zombie mới chưa nhảy nhưng đụng cây cao (isTallObstacle = true): không được nhảy, dừng ăn ngay
        PoleVaultingZombie pvzTallNut = new PoleVaultingZombie(500.0, 100.0, 0, board);
        pvzTallNut.onCollideWithTarget(normalPlant, true);
        assertInstanceOf(EatingState.class, pvzTallNut.getCurrentState());
        assertFalse(pvzTallNut.hasVaulted());
    }

    @Test
    @DisplayName("Kịch bản 5: WaterZombie chuyển sang SwimmingState khi đi vào ô WaterTile trên GameBoard")
    void testScenario5_WaterZombieSwimmingOnWaterTile() {
        WaterZombie wz = new WaterZombie(500.0, 100.0, 0, board);
        assertEquals(17.0, wz.getSpeed(), 0.001);
        assertEquals(22.0, wz.getSwimSpeed(), 0.001);
        assertTrue(wz.isWaterZombie());

        // Đặt ô nước tại lane 0, col 3 (x: 220 + 3*80 = 460)
        board.setTile(0, 3, new WaterTile(0, 3, 460.0, 100.0, 80.0, 100.0));
        wz.setX(480.0); // đặt tại tâm ô nước

        assertTrue(wz.isOnWater());
        wz.update(0.1); // Tự động phát hiện ô nước -> SwimmingState

        assertInstanceOf(SwimmingState.class, wz.getCurrentState());
        assertEquals("SWIMMING", wz.getCurrentState().getStateName());

        // Bơi sang trái với tốc độ swimSpeed = 22.0 px/s
        double prevX = wz.getX();
        wz.update(0.5);
        assertEquals(prevX - 22.0 * 0.5, wz.getX(), 0.001);

        // Di chuyển sang ô cỏ (grass) -> tự động chuyển lại WalkingState
        wz.setX(320.0);
        assertFalse(wz.isOnWater());
        wz.update(0.1);
        assertInstanceOf(WalkingState.class, wz.getCurrentState());
    }

    @Test
    @DisplayName("Kịch bản 6: Bất kỳ Zombie nào khi hp <= 0 đều chuyển sang DeadState và sau 1s thì isReadyToRemove == true")
    void testScenario6_DeadStateTransitionAndReadyToRemove() {
        zombie.takeDamage(150);
        assertTrue(zombie.isAlive());
        assertInstanceOf(WalkingState.class, zombie.getCurrentState());

        zombie.takeDamage(100);
        assertEquals(0, zombie.getHp());
        assertFalse(zombie.isAlive());
        assertInstanceOf(DeadState.class, zombie.getCurrentState());
        assertEquals("DEAD", zombie.getCurrentState().getStateName());
        assertFalse(zombie.isReadyToRemove());

        // Sau 0.5s: chưa hết thời gian ngã gục
        zombie.update(0.5);
        assertFalse(zombie.isReadyToRemove());

        // Sau tổng cộng 1.1s (> 1.0s): sẵn sàng dọn dẹp khỏi sân
        zombie.update(0.6);
        assertTrue(zombie.isReadyToRemove());
    }

    @Test
    @DisplayName("Kịch bản 7: ZombieFactory đọc đúng dữ liệu từ zombies_spec.json và tạo đủ 6 biến thể")
    void testScenario7_ZombieFactoryReadsJsonAndCreatesAllVariants() {
        Zombie normal = ZombieFactory.createZombie("NORMAL", 1, board);
        assertInstanceOf(NormalZombie.class, normal);
        assertEquals(940.0, normal.getX(), 0.001);
        assertEquals(200, normal.getHp());
        assertEquals(17.0, normal.getSpeed(), 0.001);

        Zombie flag = ZombieFactory.createZombie("FLAG", 2, board);
        assertInstanceOf(FlagZombie.class, flag);
        assertEquals(22.0, flag.getSpeed(), 0.001);

        Zombie cone = ZombieFactory.createZombie("CONEHEAD", 0, board);
        assertInstanceOf(ConeheadZombie.class, cone);
        assertEquals(370, ((ConeheadZombie) cone).getArmorHp());

        Zombie bucket = ZombieFactory.createZombie("BUCKETHEAD", 0, board);
        assertInstanceOf(BucketheadZombie.class, bucket);
        assertEquals(1100, ((BucketheadZombie) bucket).getArmorHp());

        Zombie pole = ZombieFactory.createZombie("POLE_VAULTING", 3, board);
        assertInstanceOf(PoleVaultingZombie.class, pole);
        assertEquals(500, pole.getHp());
        assertEquals(31.0, pole.getSpeed(), 0.001);

        Zombie water = ZombieFactory.createZombie("WATER", 2, board);
        assertInstanceOf(WaterZombie.class, water);
        assertEquals(22.0, water.getSwimSpeed(), 0.001);

        // Ngoại lệ khi type không hợp lệ
        assertThrows(IllegalArgumentException.class, () -> ZombieFactory.createZombie("INVALID_TYPE", 0, board));
        assertThrows(IllegalArgumentException.class, () -> ZombieFactory.createZombie(null, 0, board));
    }

    @Test
    @DisplayName("Kịch bản 8: WaveManager spawn đúng thời điểm, xóa an toàn qua Iterator, và báo isAllWavesCleared")
    void testScenario8_WaveManagerSpawnsRemovesSafelyAndClearsAllWaves() {
        List<Wave> testWaves = new ArrayList<>();

        // Wave 1: 2 zombie
        Wave wave1 = new Wave(false);
        wave1.addEntry("NORMAL", 0, 1.0);
        wave1.addEntry("CONEHEAD", 1, 2.0);
        testWaves.add(wave1);

        // Wave 2: Huge wave với 1 zombie
        Wave wave2 = new Wave(true);
        wave2.addEntry("FLAG", 2, 0.5);
        testWaves.add(wave2);

        WaveManager manager = new WaveManager(board, testWaves);
        assertEquals(2, manager.getTotalWaves());
        assertEquals(0, manager.getCurrentWaveIndex());
        assertEquals(0, manager.getActiveZombies().size());
        assertFalse(manager.isAllWavesCleared());

        // Cập nhật 0.5s -> chưa spawn zombie nào
        manager.update(0.5);
        assertEquals(0, manager.getActiveZombies().size());

        // Cập nhật thêm 0.6s (tổng 1.1s) -> Zombie 1 (NORMAL) spawn
        manager.update(0.6);
        assertEquals(1, manager.getActiveZombies().size());
        assertInstanceOf(NormalZombie.class, manager.getActiveZombies().get(0));

        // Cập nhật thêm 1.0s (tổng 2.1s) -> Zombie 2 (CONEHEAD) spawn
        manager.update(1.0);
        assertEquals(2, manager.getActiveZombies().size());

        // Tiêu diệt cả 2 zombie của Wave 1
        for (Zombie z : manager.getActiveZombies()) {
            z.takeDamage(1000);
            assertEquals("DEAD", z.getCurrentState().getStateName());
        }

        // Cho thời gian DeadState trôi qua 1.0s -> trở thành readyToRemove
        // WaveManager.update() dùng Iterator.remove() an toàn không ném ConcurrentModificationException
        assertDoesNotThrow(() -> manager.update(1.1));
        assertEquals(0, manager.getActiveZombies().size());

        // Wave 1 đã xong, tự động chuyển sang Wave 2 (Huge wave)
        assertEquals(1, manager.getCurrentWaveIndex());
        assertTrue(manager.isHugeWaveApproaching());

        // Cập nhật 0.6s -> Flag zombie của Wave 2 spawn
        manager.update(0.6);
        assertEquals(1, manager.getActiveZombies().size());
        assertInstanceOf(FlagZombie.class, manager.getActiveZombies().get(0));

        // Tiêu diệt Flag zombie
        manager.getActiveZombies().get(0).takeDamage(1000);
        manager.update(1.1);

        // Toàn bộ các wave đã được hoàn thành sạch sẽ
        assertEquals(0, manager.getActiveZombies().size());
        assertTrue(manager.isAllWavesCleared());
        assertEquals(1.0, manager.getProgressRatio(), 0.001);
    }

    @Test
    @DisplayName("WaveManager mặc định tạo đủ 3 wave với độ khó tăng dần và đợt cuối là Huge Wave")
    void testWaveManagerDefaultWaves() {
        WaveManager defaultManager = new WaveManager(board);
        assertEquals(3, defaultManager.getTotalWaves());
        assertFalse(defaultManager.getWaves().get(0).isHugeWave());
        assertFalse(defaultManager.getWaves().get(1).isHugeWave());
        assertTrue(defaultManager.getWaves().get(2).isHugeWave());
    }

    @Test
    @DisplayName("Kịch bản 9: Tung xúc xắc canSwim bằng Random seed cố định & phân nhánh SwimmingState vs DrowningState (kiểm tra nước trước cây)")
    void testScenario9_CanSwimRollAndWaterFirstCheck() {
        // Dùng seed cố định tìm roll < 0.8 (chết đuối) và roll >= 0.8 (biết bơi)
        Random fixedSeed1 = new Random(42L);
        double firstRoll = new Random(42L).nextDouble(); // ~0.7275 < 0.8 -> canSwim = false
        assertTrue(firstRoll < 0.8, "Expected seed 42 to roll < 0.8 for NORMAL drownChance");

        Zombie drowningZombie = ZombieFactory.createZombie("NORMAL", 0, board, fixedSeed1);
        assertFalse(drowningZombie.canSwim());

        // Tìm seed cho roll >= 0.8 -> canSwim = true
        long swimSeed = -1L;
        for (long s = 1L; s < 100_000L; s += 7919L) {
            if (new Random(s).nextDouble() >= 0.8) {
                swimSeed = s;
                break;
            }
        }
        assertTrue(swimSeed != -1L && new Random(swimSeed).nextDouble() >= 0.8);
        Zombie swimmingZombie = ZombieFactory.createZombie("NORMAL", 0, board, new Random(swimSeed));
        assertTrue(swimmingZombie.canSwim());

        // WaterZombie luôn có canSwim = true bất kể seed
        Zombie waterZombie = ZombieFactory.createZombie("WATER", 0, board, new Random(42L));
        assertTrue(waterZombie.canSwim());

        // Lắng nghe sự kiện ZombieDeathEvent(DROWNED)
        AtomicReference<ZombieDeathEvent> drownedEvent = new AtomicReference<>();
        CombatListener<ZombieDeathEvent> listener = drownedEvent::set;
        CombatEventBus.getInstance().subscribe(ZombieDeathEvent.class, listener);

        try {
            // Đặt ô nước tại lane 0, col 3 (x = 460..540) và một cây giả ngay trên ô nước
            TerrainQuery waterTerrain = (lane, x) -> (x >= 460.0 && x < 540.0) ? TileType.WATER : TileType.GRASS;
            drowningZombie.setTerrainQuery(waterTerrain);
            drowningZombie.setX(480.0);

            DummyPlant plantOnWater = new DummyPlant(480.0, 100.0, 100);
            // Gắn target sống, nhưng kiểm tra nước phải chạy TRƯỚC va chạm cây
            drowningZombie.setCurrentTarget(plantOnWater);
            drowningZombie.update(0.1);

            assertInstanceOf(DrowningState.class, drowningZombie.getCurrentState());
            assertEquals("DROWNING", drowningZombie.getCurrentState().getStateName());
            double xDuringDrown = drowningZombie.getX();

            // Trong DrowningState, zombie đứng yên ngang và sau 1.0s chuyển sang DeadState + phát sự kiện DROWNED
            drowningZombie.update(1.0);
            assertEquals(xDuringDrown, drowningZombie.getX(), 0.001);
            assertTrue(drowningZombie.isDrowned());
            assertInstanceOf(DeadState.class, drowningZombie.getCurrentState());
            assertNotNull(drownedEvent.get());
            assertEquals(ZombieDeathEvent.DeathCause.DROWNED, drownedEvent.get().getCause());
            assertEquals(drowningZombie, drownedEvent.get().getZombie());

            // Kiểm tra zombie biết bơi đi vào ô nước -> chuyển sang SwimmingState
            swimmingZombie.setTerrainQuery(waterTerrain);
            swimmingZombie.setX(480.0);
            swimmingZombie.update(0.1);
            assertInstanceOf(SwimmingState.class, swimmingZombie.getCurrentState());
        } finally {
            CombatEventBus.getInstance().unsubscribe(ZombieDeathEvent.class, listener);
        }
    }

    @Test
    @DisplayName("Kịch bản 10: EatingState ăn tầng TOP trước tầng BASE (LilyPad) và quay về SwimmingState khi ở trên nước")
    void testScenario10_TwoLayerCellEatingOrderAndReturnToSwimming() {
        WaterZombie wz = new WaterZombie(480.0, 100.0, 0, board);
        board.setTile(0, 3, new WaterTile(0, 3, 460.0, 100.0, 80.0, 100.0));
        wz.changeState(new SwimmingState());

        DummyPlant topPlant = new DummyPlant(480.0, 100.0, 25);  // Peashooter (TOP)
        DummyPlant basePlant = new DummyPlant(480.0, 100.0, 25); // LilyPad (BASE)

        LayeredTarget cell = new LayeredTarget() {
            @Override
            public Entity getTopPlant() {
                return topPlant;
            }

            @Override
            public Entity getBasePlant() {
                return basePlant;
            }
        };

        wz.onCollideWithCell(cell, false);
        assertInstanceOf(EatingState.class, wz.getCurrentState());
        assertSame(topPlant, wz.getCurrentTarget());

        // Cắn lần 1 (1.0s): ăn hết tầng TOP, tầng BASE vẫn nguyên vẹn và trở thành mục tiêu mới
        wz.update(1.0);
        assertFalse(topPlant.isAlive());
        assertTrue(basePlant.isAlive());
        assertEquals(25, basePlant.getHp());
        assertSame(basePlant, wz.getCurrentTarget());
        assertInstanceOf(EatingState.class, wz.getCurrentState());

        // Cắn lần 2 (1.0s): ăn hết tầng BASE -> ô trống -> kiểm tra lại địa hình là WATER -> quay về SwimmingState
        wz.update(1.0);
        assertFalse(basePlant.isAlive());
        assertNull(wz.getCurrentTarget());
        assertInstanceOf(SwimmingState.class, wz.getCurrentState());
    }

    @Test
    @DisplayName("Kịch bản 11: EscalatingRandomSpawnPolicy tính đúng interval(n), tuân thủ ân hạn, FlagZombie đầu turn và trần maxActiveZombies")
    void testScenario11_EscalatingRandomSpawnPolicyWithFixedSeed() {
        SpawnSpec customSpec = new SpawnSpec(
                5.0,   // gracePeriod
                4.0,   // T0
                1.0,   // T_min
                0.5,   // decay
                3,     // maxActiveZombies
                3,     // zombiesPerTurn
                Arrays.asList("NORMAL", "CONEHEAD")
        );

        EscalatingRandomSpawnPolicy policy = new EscalatingRandomSpawnPolicy(customSpec, 2, new Random(12345L));
        WaveManager manager = new WaveManager(board, policy, new Random(12345L));

        // Kiểm tra công thức interval(n) = Math.max(T_min, T0 * Math.pow(decay, n))
        assertEquals(4.0, policy.computeInterval(0), 0.001);
        assertEquals(2.0, policy.computeInterval(1), 0.001);
        assertEquals(1.0, policy.computeInterval(2), 0.001);
        assertEquals(1.0, policy.computeInterval(5), 0.001); // clamped by T_min

        // Trong thời gian ân hạn (< 5.0s): chưa spawn zombie nào
        manager.update(4.5);
        assertEquals(0, manager.getActiveZombies().size());
        assertFalse(manager.isAllWavesCleared());

        // Vừa hết thời gian ân hạn (tổng 5.0s): zombie đầu tiên của turn 0 là FlagZombie
        manager.update(0.5);
        assertEquals(1, manager.getActiveZombies().size());
        assertInstanceOf(FlagZombie.class, manager.getActiveZombies().get(0));
        assertTrue(manager.isHugeWaveApproaching());

        // Sau interval(0) = 4.0s: spawn zombie thứ 2
        manager.update(4.0);
        assertEquals(2, manager.getActiveZombies().size());

        // Sau interval(1) = 2.0s: spawn zombie thứ 3 (đạt trần maxActiveZombies = 3, kết thúc turn 0)
        manager.update(2.0);
        assertEquals(3, manager.getActiveZombies().size());
        assertEquals(1, manager.getCurrentWaveIndex());

        // Sau interval(2) = 1.0s: đã chạm trần maxActiveZombies = 3 nên KHÔNG spawn thêm
        manager.update(1.0);
        assertEquals(3, manager.getActiveZombies().size());

        // Giết 1 zombie và cho qua DeadState (1.0s) để dọn khỏi sân -> còn 2 zombie
        manager.getActiveZombies().get(0).takeDamage(1000);
        manager.update(1.1);
        assertEquals(2, manager.getActiveZombies().size());

        // Frame tiếp theo khi slot đã trống (< 3): FlagZombie của turn 1 lập tức spawn
        manager.update(0.1);
        assertEquals(3, manager.getActiveZombies().size());
        assertInstanceOf(FlagZombie.class, manager.getActiveZombies().get(2));

        // Kiểm tra chế độ vô hạn (maxTurns == null) không bao giờ báo isAllWavesCleared()
        EscalatingRandomSpawnPolicy endlessPolicy = new EscalatingRandomSpawnPolicy(customSpec, null, new Random(99L));
        assertFalse(endlessPolicy.isAllWavesCleared(List.of()));
    }
}
