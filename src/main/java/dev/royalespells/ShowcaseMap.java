package dev.royalespells;

import dev.royalespells.entity.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.util.*;

/** Self-contained recording set. Its persistent marker prevents edits in ordinary worlds. */
public final class ShowcaseMap {
    public record Scene(Spell spell,String title,String hint,Block accent,Block floor){}
    public static final List<Scene> SCENES=List.of(
        s(Spell.ZAP,"电击法术 · 闪电试场","瞄准金色中心点：短促落雷与分叉电弧",Blocks.CYAN_CONCRETE,Blocks.POLISHED_ANDESITE),
        s(Spell.ZAP_EVOLUTION,"觉醒电击 · 双重脉冲","内圈先命中，第二次扩大到外圈目标",Blocks.PURPLE_CONCRETE,Blocks.POLISHED_ANDESITE),
        s(Spell.ARROWS,"万箭齐发 · 骷髅军阵","三轮箭雨，观察最外侧骷髅",Blocks.RED_CONCRETE,Blocks.SMOOTH_SANDSTONE),
        s(Spell.FIREBALL,"火球 · 桥头阻击","向桥头密集队伍投出火球",Blocks.ORANGE_CONCRETE,Blocks.SMOOTH_SANDSTONE),
        s(Spell.LIGHTNING,"雷电法术 · 三塔守卫","优先劈中三个高血量目标",Blocks.BLUE_CONCRETE,Blocks.STONE_BRICKS),
        s(Spell.ROCKET,"火箭 · 重型靶场","保持远景可拍到抬头升空和低头俯冲",Blocks.RED_CONCRETE,Blocks.POLISHED_ANDESITE),
        s(Spell.POISON,"毒药 · 沼泽围困","毒雾持续削弱被围住的队伍",Blocks.YELLOW_TERRACOTTA,Blocks.MOSS_BLOCK),
        s(Spell.FREEZE,"冰冻 · 极寒哨站","冰层包裹守军，冻结后近距离拍摄",Blocks.LIGHT_BLUE_CONCRETE,Blocks.SNOW_BLOCK),
        s(Spell.RAGE,"狂暴 · 野蛮人冲锋","向金色圆心施法：友军染色并加速进攻",Blocks.MAGENTA_CONCRETE,Blocks.SMOOTH_SANDSTONE),
        s(Spell.THE_LOG,"复仇滚木 · 狭道防线","站在蓝色起点，沿正前方直线释放",Blocks.GREEN_CONCRETE,Blocks.MOSS_BLOCK),
        s(Spell.EARTHQUAKE,"地震 · 木屋与石墙","连续三震：木屋倒塌，石墙裂纹短暂保留",Blocks.ORANGE_TERRACOTTA,Blocks.GRASS_BLOCK),
        s(Spell.GIANT_SNOWBALL,"巨型雪球 · 冰桥推退","向中心投掷，观察击退与减速",Blocks.WHITE_CONCRETE,Blocks.SNOW_BLOCK),
        s(Spell.GIANT_SNOWBALL_EVOLUTION,"觉醒雪球 · 滚动裹挟","雪球命中后继续前滚并携带目标",Blocks.LIGHT_BLUE_CONCRETE,Blocks.SNOW_BLOCK),
        s(Spell.TORNADO,"飓风 · 环形包围","将分散在周围的守军拉向中心",Blocks.CYAN_TERRACOTTA,Blocks.POLISHED_ANDESITE),
        s(Spell.GOBLIN_BARREL,"小僵尸飞桶 · 城门突袭","飞桶落地后召唤三只小僵尸",Blocks.LIME_CONCRETE,Blocks.MOSS_BLOCK),
        s(Spell.GOBLIN_BARREL_EVOLUTION,"觉醒飞桶 · 双路佯攻","双桶分别落向相邻的两座哨台",Blocks.PURPLE_CONCRETE,Blocks.MOSS_BLOCK),
        s(Spell.BARBARIAN_BARREL,"野蛮人滚桶 · 破阵","向前滚动后，野蛮人持剑出场",Blocks.YELLOW_CONCRETE,Blocks.SMOOTH_SANDSTONE),
        s(Spell.BARBARIAN_BARREL_HERO,"英雄滚桶 · 再次冲锋","普通右键部署；潜行右键卡牌再次翻滚",Blocks.ORANGE_CONCRETE,Blocks.SMOOTH_SANDSTONE),
        s(Spell.ROYAL_DELIVERY,"皇家速递 · 木桶卫队","从天而降的箱子与木桶头皇家卫队",Blocks.BLUE_CONCRETE,Blocks.STONE_BRICKS),
        s(Spell.GRAVEYARD,"墓园 · 古堡围攻","墓碑之间持续出现持石剑的骷髅",Blocks.PURPLE_TERRACOTTA,Blocks.MOSSY_STONE_BRICKS),
        s(Spell.CLONE,"克隆 · 镜池分身","对准友军：生成青色半透明复制体",Blocks.CYAN_CONCRETE,Blocks.QUARTZ_BLOCK),
        s(Spell.MIRROR,"镜像 · 两次轰击","先用第 1 格火球，再用第 2 格镜像",Blocks.PURPLE_CONCRETE,Blocks.QUARTZ_BLOCK),
        s(Spell.GOBLIN_CURSE,"小僵尸诅咒 · 感染营地","低血量目标被诅咒击倒后转化为小僵尸",Blocks.LIME_TERRACOTTA,Blocks.MOSS_BLOCK),
        s(Spell.VOID,"虚空 · 孤立重击","对单个高血量目标展示三次下劈",Blocks.RED_TERRACOTTA,Blocks.POLISHED_BLACKSTONE),
        s(Spell.VINES,"藤蔓 · 林地禁锢","实体藤蔓缠绕并限制三个强壮目标",Blocks.GREEN_TERRACOTTA,Blocks.MOSS_BLOCK),
        s(Spell.PARTY_ROCKET,"派对火箭 · 终场庆典","火箭落地后，队伍变成小僵尸",Blocks.PINK_CONCRETE,Blocks.QUARTZ_BLOCK)
    );
    private static Scene s(Spell spell,String title,String hint,Block accent,Block floor){return new Scene(spell,title,hint,accent,floor);}
    public static final int Y=64,SPACING=112;
    private static final String KEY="royale_recording_map",ACTOR="royale_scene_actor",FRIEND="royale_scene_friend";
    private static final Map<UUID,Long> COOLDOWNS=new HashMap<>();
    private static final Set<UUID> ACTIVE=new HashSet<>();
    private static final Map<MinecraftServer,Integer> BUILDING=new HashMap<>();
    public static final class State extends PersistentState {
        public boolean enabled,ready;public int index;
        @Override public NbtCompound writeNbt(NbtCompound n){n.putBoolean("Enabled",enabled);n.putBoolean("Ready",ready);n.putInt("Index",index);return n;}
        static State read(NbtCompound n){var s=new State();s.enabled=n.getBoolean("Enabled");s.ready=n.getBoolean("Ready");s.index=Math.floorMod(n.getInt("Index"),SCENES.size());return s;}
    }
    private static State state(ServerWorld world){return world.getPersistentStateManager().getOrCreate(State::read,State::new,KEY);}
    public static boolean enabled(ServerWorld world){return state(world).enabled;}
    public static boolean ready(ServerWorld world){return state(world).ready;}
    public static int index(ServerWorld world){return state(world).index;}
    public static BlockPos center(int index){return new BlockPos((index%6)*SPACING,Y,(index/6)*SPACING);}
    private static Box bounds(int index){var c=center(index);return new Box(c.add(-25,-5,-25),c.add(26,38,26));}
    public static void install(){
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            var world=handler.player.getServerWorld();if(enabled(world)&&ready(world))enter(handler.player,index(world));
        });
        ServerTickEvents.END_SERVER_TICK.register(ShowcaseMap::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->{BUILDING.remove(server);COOLDOWNS.clear();ACTIVE.clear();});
    }
    public static void beginBuild(ServerWorld world){
        if(!Boolean.getBoolean("royalespells.buildShowcase")||!(world.getChunkManager().getChunkGenerator() instanceof net.minecraft.world.gen.chunk.FlatChunkGenerator))
            throw new IllegalStateException("Recording map generation requires the isolated flat-world builder");
        var st=state(world);st.enabled=true;st.ready=false;st.markDirty();
        var server=world.getServer();
        for(var rule:List.of(GameRules.DO_MOB_SPAWNING,GameRules.DO_DAYLIGHT_CYCLE,GameRules.DO_WEATHER_CYCLE,GameRules.DO_FIRE_TICK,GameRules.DO_MOB_GRIEFING,GameRules.DO_PATROL_SPAWNING,GameRules.DO_TRADER_SPAWNING))world.getGameRules().get(rule).set(false,server);
        world.getGameRules().get(GameRules.KEEP_INVENTORY).set(true,server);world.getGameRules().get(GameRules.RANDOM_TICK_SPEED).set(0,server);
        world.setTimeOfDay(6000);world.setWeather(0,0,false,false);world.setSpawnPos(center(0).add(0,2,-12),0);
        BUILDING.put(server,0);
    }
    private static void tick(MinecraftServer server){
        if(BUILDING.containsKey(server)){
            int i=BUILDING.get(server);var w=server.getOverworld();buildArena(w,i);
            System.out.println("ROYALE_MAP_BUILD "+(i+1)+"/"+SCENES.size());
            if(i+1==SCENES.size()){
                BUILDING.remove(server);var st=state(w);st.ready=true;st.markDirty();
                for(var p:server.getPlayerManager().getPlayerList())enter(p,0);
            }else BUILDING.put(server,i+1);
        }
        for(var p:server.getPlayerManager().getPlayerList()){
            var w=p.getServerWorld();if(!enabled(w)||!ready(w))continue;
            for(var mob:w.getEntitiesByClass(MobEntity.class,bounds(index(w)),e->e.getCommandTags().contains(ACTOR)))mob.setFireTicks(0);
            if(p.getY()<Y-12){enter(p,index(w));continue;}
            // Staged friends wait for the first cast; actual spell and troop AI then run normally.
            for(var fx:w.getEntitiesByClass(SpellEntity.class,bounds(index(w)),e->p.getUuid().equals(e.ownerId))){
                if(!ACTIVE.add(fx.getUuid()))continue;
                for(var mob:w.getEntitiesByClass(MobEntity.class,bounds(index(w)),e->e.getCommandTags().contains(FRIEND)))mob.setAiDisabled(false);
            }
        }
        if(server.getTicks()%200==0)ACTIVE.clear();
    }
    public static void switchScene(ServerPlayerEntity player,int direction){
        var world=player.getServerWorld();if(!enabled(world)||!ready(world)){player.sendMessage(Text.literal("请在「皇室法术 · 录制片场」存档中使用场景控制。"),true);return;}
        long now=world.getTime();if(COOLDOWNS.getOrDefault(player.getUuid(),-100L)>now)return;
        COOLDOWNS.put(player.getUuid(),now+12);enter(player,Math.floorMod(index(world)+direction,SCENES.size()));
    }
    public static void enter(ServerPlayerEntity player,int index){
        var world=player.getServerWorld();if(!enabled(world)||!ready(world))return;
        cleanup(world,ShowcaseMap.index(world));
        var st=state(world);st.index=Math.floorMod(index,SCENES.size());st.markDirty();index=st.index;
        cleanup(world,index);resetSet(world,index);setupActors(world,player,index);
        var scene=SCENES.get(index);var c=center(index);
        player.changeGameMode(GameMode.CREATIVE);player.clearStatusEffects();player.setFireTicks(0);
        double start=scene.spell().rolling()?(scene.spell()==Spell.THE_LOG?-8.5:-4.5):-12;
        double height=scene.spell().rolling()?Y:Y+2;
        player.teleport(world,c.getX()+.5,height,c.getZ()+start,0,scene.spell().rolling()?5:16.15f);
        player.getAbilities().flying=!scene.spell().rolling();player.sendAbilitiesUpdate();
        player.setSpawnPoint(world.getRegistryKey(),c.add(0,2,-12),0,true,false);
        var inv=player.getInventory();inv.clear();
        if(scene.spell()==Spell.MIRROR){inv.setStack(0,new ItemStack(RoyaleSpells.ITEMS.get(Spell.FIREBALL)));inv.setStack(1,new ItemStack(RoyaleSpells.ITEMS.get(Spell.MIRROR)));}
        else inv.setStack(0,new ItemStack(RoyaleSpells.ITEMS.get(scene.spell())));
        inv.setStack(7,new ItemStack(RoyaleSpells.PREVIOUS_SCENE));inv.setStack(8,new ItemStack(RoyaleSpells.NEXT_SCENE));inv.selectedSlot=0;
        player.currentScreenHandler.sendContentUpdates();player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(0));SpellEngine.resetForRecording(player);
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(5,35,10));
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(String.format(Locale.ROOT,"%02d / %02d   ",index+1,SCENES.size())+scene.title()).formatted(Formatting.GOLD)));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(scene.hint()).formatted(Formatting.WHITE)));
    }
    private static void cleanup(ServerWorld w,int i){
        var box=bounds(i);EarthquakeDestruction.clearRegion(w,box);
        for(var e:w.getOtherEntities(null,box,e->!(e instanceof net.minecraft.entity.player.PlayerEntity)&&(e.getCommandTags().contains(ACTOR)||e instanceof Summoned||e instanceof SpellEntity||e instanceof net.minecraft.entity.ItemEntity||e instanceof net.minecraft.entity.projectile.ProjectileEntity)))e.discard();
    }
    private static void put(ServerWorld w,BlockPos c,int x,int y,int z,Block b){w.setBlockState(c.add(x,y,z),b.getDefaultState(),2);}
    private static void fill(ServerWorld w,BlockPos c,int x1,int y1,int z1,int x2,int y2,int z2,Block b){for(var p:BlockPos.iterate(c.add(x1,y1,z1),c.add(x2,y2,z2)))w.setBlockState(p,b.getDefaultState(),2);}
    private static void text(ServerWorld w,Vec3d p,String message,float scale){
        var e=EntityType.TEXT_DISPLAY.create(w);var n=new NbtCompound();e.writeNbt(n);
        n.putString("text",Text.Serializer.toJson(Text.literal(message)));n.putString("billboard","center");n.putInt("line_width",360);n.putInt("background",0x90202b38);n.putByte("text_opacity",(byte)255);n.putBoolean("shadow",true);
        var transform=new NbtList();for(int i=0;i<16;i++)transform.add(NbtFloat.of(i==15?1:i==0||i==5||i==10?scale:0));n.put("transformation",transform);
        var brightness=new NbtCompound();brightness.putInt("block",15);brightness.putInt("sky",15);n.put("brightness",brightness);
        e.readNbt(n);e.setPosition(p);w.spawnEntity(e);
    }
    private static void tower(ServerWorld w,BlockPos c,int x,int z,Block accent){
        fill(w,c,x-2,-1,z-2,x+2,8,z+2,Blocks.STONE_BRICKS);fill(w,c,x-2,8,z-2,x+2,9,z+2,accent);
        for(int dx=-2;dx<=2;dx+=2)for(int dz=-2;dz<=2;dz+=2)put(w,c,x+dx,10,z+dz,Blocks.STONE_BRICKS);
        fill(w,c,x,10,z,x,14,z,Blocks.OAK_FENCE);fill(w,c,x+1,12,z,x+4,14,z,accent);fill(w,c,x+1,12,z,x+1,14,z,Blocks.YELLOW_CONCRETE);
        put(w,c,x,6,z-3,Blocks.SEA_LANTERN);
    }
    private static void buildArena(ServerWorld w,int i){
        var c=center(i);var s=SCENES.get(i);
        for(int x=-24;x<=24;x++)for(int z=-24;z<=24;z++){
            if(Math.abs(x)+Math.abs(z)>43)continue;
            put(w,c,x,-3,z,Blocks.DEEPSLATE_BRICKS);put(w,c,x,-2,z,Blocks.STONE_BRICKS);
            put(w,c,x,-1,z,Math.max(Math.abs(x),Math.abs(z))>=22?s.accent():s.floor());
        }
        for(int side:new int[]{-1,1}){
            fill(w,c,side*22,0,-18,side*22,5,18,Blocks.STONE_BRICKS);
            for(int z=-18;z<=18;z+=4){fill(w,c,side*22,6,z,side*22,6,z+1,s.accent());put(w,c,side*21,3,z,Blocks.SEA_LANTERN);}
            for(int step=0;step<3;step++)fill(w,c,side>0?17+step:-19,step,-14,side>0?19:-17-step,step,14,step%2==0?s.accent():Blocks.SMOOTH_STONE);
            tower(w,c,side*20,20,s.accent());tower(w,c,side*20,-20,s.accent());
        }
        fill(w,c,-18,0,22,18,7,22,Blocks.STONE_BRICKS);fill(w,c,-18,7,22,18,8,22,s.accent());
        for(int x=-16;x<=16;x+=4)fill(w,c,x,9,22,x+1,9,22,Blocks.STONE_BRICKS);
        // Blue arrival carpet, gold lane accents, and a recognizable crowned arena backdrop.
        fill(w,c,-2,-1,-20,2,-1,-10,Blocks.BLUE_CONCRETE);
        for(int x:new int[]{-3,3})fill(w,c,x,-1,-20,x,-1,-10,Blocks.YELLOW_CONCRETE);
        fill(w,c,-3,10,22,3,11,22,Blocks.GOLD_BLOCK);
        for(int x:new int[]{-3,0,3})fill(w,c,x,12,22,x,14-(x==0?0:1),22,Blocks.GOLD_BLOCK);
        text(w,Vec3d.ofBottomCenter(c.add(0,7,20)),String.format(Locale.ROOT,"%02d  /  %02d\n",i+1,SCENES.size())+s.title(),3f);
        text(w,Vec3d.ofBottomCenter(c.add(0,4,-21)),"录制片场\n第 8 格：上一场景  ·  第 9 格：下一场景\n潜行 + 使用控制物品：重置当前场景",.7f);
        resetSet(w,i);
    }
    private static void resetSet(ServerWorld w,int i){
        var c=center(i);var scene=SCENES.get(i);var spell=scene.spell();
        fill(w,c,-10,0,-9,10,25,11,Blocks.AIR);
        fill(w,c,-10,-3,-9,10,-2,11,Blocks.DEEPSLATE_BRICKS);fill(w,c,-10,-1,-9,10,-1,11,scene.floor());
        // The small gold aiming inlay sits under targets and stays legible in clean HUD-free shots.
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(Math.abs(x)+Math.abs(z)==1)put(w,c,x,-1,z,Blocks.CUT_SANDSTONE);
        put(w,c,0,-1,0,Blocks.GOLD_BLOCK);
        switch(spell){
            case FIREBALL,GIANT_SNOWBALL,GIANT_SNOWBALL_EVOLUTION -> {
                fill(w,c,-9,-1,2,9,-1,5,spell==Spell.FIREBALL?Blocks.WATER:Blocks.PACKED_ICE);
                fill(w,c,-3,-1,-2,3,-1,7,spell==Spell.FIREBALL?Blocks.SMOOTH_STONE:Blocks.SNOW_BLOCK);
                for(int x:new int[]{-4,4})for(int z=-2;z<=7;z+=3)fill(w,c,x,0,z,x,1,z,Blocks.STONE_BRICK_WALL);
            }
            case EARTHQUAKE -> {
                for(int x=-2;x<=0;x++)for(int z=0;z<=2;z++)for(int y=0;y<=3;y++)if(x==-2||x==0||z==0||z==2||y==3)put(w,c,x,y,z,Blocks.OAK_PLANKS);
                fill(w,c,-1,0,0,-1,1,0,Blocks.AIR);fill(w,c,-3,4,-1,1,4,3,Blocks.SPRUCE_SLAB);
                tree(w,c,2,-1,false);fill(w,c,-2,0,3,2,2,3,Blocks.STONE_BRICKS);
                put(w,c,0,-1,-1,Blocks.GOLD_BLOCK);
            }
            case GRAVEYARD -> {
                for(int a=0;a<8;a++){double angle=a*Math.PI/4;int x=(int)Math.round(Math.cos(angle)*6),z=(int)Math.round(Math.sin(angle)*6);fill(w,c,x,0,z,x,1,z,Blocks.COBBLESTONE_WALL);put(w,c,x,2,z,Blocks.STONE_BRICK_SLAB);}
                fill(w,c,-2,-1,7,2,2,10,Blocks.CHISELED_STONE_BRICKS);fill(w,c,-1,0,6,1,1,6,Blocks.IRON_BARS);
            }
            case THE_LOG,BARBARIAN_BARREL,BARBARIAN_BARREL_HERO -> {
                for(int x:new int[]{-3,3}){fill(w,c,x,0,-7,x,1,7,Blocks.OAK_FENCE);tree(w,c,x*2,6,false);}
                fill(w,c,-1,-1,-8,1,-1,8,Blocks.DIRT_PATH);
            }
            case POISON,GOBLIN_CURSE -> {
                for(int x:new int[]{-7,7}){fill(w,c,x,-1,-4,x+1,-1,5,Blocks.WATER);tree(w,c,x,7,true);}
                for(int x:new int[]{-4,4}){put(w,c,x,0,5,Blocks.CAULDRON);put(w,c,x,0,-4,Blocks.BROWN_MUSHROOM);}
            }
            case VINES -> {for(int x:new int[]{-7,7})for(int z:new int[]{-3,6})tree(w,c,x,z,true);}
            case VOID -> {
                for(int x:new int[]{-7,7}){fill(w,c,x,0,5,x,5,5,Blocks.POLISHED_BLACKSTONE);put(w,c,x,6,5,Blocks.REDSTONE_BLOCK);}
                for(int z=-5;z<=5;z++)for(int x:new int[]{-5,5})put(w,c,x,-1,z,Blocks.RED_NETHER_BRICKS);
            }
            case CLONE,MIRROR -> {for(int x:new int[]{-6,6})fill(w,c,x,-1,-5,x+1,-1,6,Blocks.WATER);}
            case LIGHTNING -> {for(int x:new int[]{-5,0,5}){fill(w,c,x,0,8,x,4,8,Blocks.CHISELED_STONE_BRICKS);put(w,c,x,5,8,Blocks.LIGHTNING_ROD);}}
            case GOBLIN_BARREL_EVOLUTION -> {for(int x:new int[]{0,5})fill(w,c,x-1,-1,2,x+1,-1,4,Blocks.STONE_BRICKS);}
            case FREEZE -> {for(int x:new int[]{-7,7}){fill(w,c,x,0,5,x,4,5,Blocks.PACKED_ICE);put(w,c,x,5,5,Blocks.SNOW_BLOCK);}}
            default -> {}
        }
    }
    private static void tree(ServerWorld w,BlockPos c,int x,int z,boolean lush){
        fill(w,c,x,0,z,x,6,z,Blocks.OAK_LOG);
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int y=4;y<=7;y++)if(Math.abs(dx)+Math.abs(dz)<=(y==7?1:3))w.setBlockState(c.add(x+dx,y,z+dz),(lush?Blocks.AZALEA_LEAVES:Blocks.OAK_LEAVES).getDefaultState().with(LeavesBlock.PERSISTENT,true),2);
    }
    private static MobEntity target(ServerWorld w,BlockPos c,EntityType<? extends MobEntity> type,double x,double z,float hp){
        var mob=type.create(w);mob.refreshPositionAndAngles(c.getX()+.5+x,Y,c.getZ()+.5+z,180,0);mob.setBodyYaw(180);mob.setHeadYaw(180);mob.setAiDisabled(true);mob.setPersistent();mob.setSilent(true);mob.setOnGround(true);mob.addCommandTag(ACTOR);
        var max=mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);if(max!=null)max.setBaseValue(hp);mob.setHealth(hp);
        w.spawnEntity(mob);return mob;
    }
    private static void friend(ServerWorld w,ServerPlayerEntity p,BlockPos c,String kind,double x,double z){
        var mob=SpellEngine.summon(w,p.getUuid(),new Vec3d(c.getX()+.5+x,Y,c.getZ()+.5+z),kind,false);
        if(mob==null)throw new IllegalStateException("Recording friend spawn blocked");
        ((Summoned)mob).setup(p.getUuid(),72000,false);mob.setAiDisabled(true);mob.setYaw(0);mob.setHeadYaw(0);mob.setBodyYaw(0);mob.addCommandTag(ACTOR);mob.addCommandTag(FRIEND);
    }
    private static void setupActors(ServerWorld w,ServerPlayerEntity player,int i){
        var c=center(i);var spell=SCENES.get(i).spell();
        switch(spell){
            case ZAP,ZAP_EVOLUTION -> {for(double[] p:new double[][]{{-1,0},{1,0},{0,1.4},{-2.65,.3},{2.65,.3}})target(w,c,EntityType.HUSK,p[0],p[1],16);}
            case ARROWS -> {for(double x:new double[]{-4.2,-2.8,-1.4,0,1.4,2.8,4.2})for(double z:new double[]{-.4,1.1})target(w,c,EntityType.SKELETON,x,z,8);}
            case FIREBALL,MIRROR -> {for(double x:new double[]{-2.6,-1.3,0,1.3,2.6})target(w,c,EntityType.HUSK,x,0,24);}
            case LIGHTNING -> {for(double x:new double[]{-2,0,2})target(w,c,EntityType.IRON_GOLEM,x,0,100);for(double z:new double[]{-1.5,1.5})target(w,c,EntityType.HUSK,0,z,20);}
            case RAGE,CLONE -> {friend(w,player,c,"barbarian",-1,0);friend(w,player,c,"barbarian",1,0);friend(w,player,c,"recruit",0,1.5);for(double x:new double[]{-2,2})target(w,c,EntityType.IRON_GOLEM,x,7,100);}
            case THE_LOG -> {for(int z=-5;z<=3;z+=2){target(w,c,EntityType.SKELETON,-.7,z,8);target(w,c,EntityType.SKELETON,.7,z,8);}}
            case BARBARIAN_BARREL,BARBARIAN_BARREL_HERO -> {target(w,c,EntityType.SKELETON,0,-1.5,6);target(w,c,EntityType.HUSK,0,5,40);}
            case EARTHQUAKE -> {}
            case TORNADO -> {for(int j=0;j<8;j++){double a=j*Math.PI/4+Math.PI/8;target(w,c,EntityType.HUSK,Math.cos(a)*4.5,Math.sin(a)*4.5,24);}}
            case GRAVEYARD -> target(w,c,EntityType.IRON_GOLEM,0,0,180);
            case VOID -> target(w,c,EntityType.IRON_GOLEM,0,0,100);
            case VINES -> {for(double x:new double[]{-2,0,2})target(w,c,EntityType.IRON_GOLEM,x,0,100);}
            case GOBLIN_CURSE -> {for(double x:new double[]{-2,-1,0,1,2})target(w,c,EntityType.HUSK,x,0,4);}
            case GOBLIN_BARREL_EVOLUTION -> {target(w,c,EntityType.HUSK,0,3,45);target(w,c,EntityType.HUSK,5,3,45);}
            case GOBLIN_BARREL,ROYAL_DELIVERY -> {target(w,c,EntityType.HUSK,-1,2,40);target(w,c,EntityType.HUSK,1,2,40);}
            case ROCKET -> {target(w,c,EntityType.IRON_GOLEM,0,0,100);target(w,c,EntityType.HUSK,-1.5,.5,30);target(w,c,EntityType.HUSK,1.5,.5,30);}
            default -> {for(double x:new double[]{-1.8,0,1.8})target(w,c,EntityType.HUSK,x,.5,spell==Spell.PARTY_ROCKET?20:32);}
        }
    }
}
