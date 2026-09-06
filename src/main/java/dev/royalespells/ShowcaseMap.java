package dev.royalespells;

import net.minecraft.nbt.*;
import net.minecraft.world.*;
import dev.royalespells.entity.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
    public static final class State extends SavedData {
        public boolean enabled,ready;public int index;
        @Override public CompoundTag save(CompoundTag n,net.minecraft.core.HolderLookup.Provider registries){n.putBoolean("Enabled",enabled);n.putBoolean("Ready",ready);n.putInt("Index",index);return n;}
        static State read(CompoundTag n){var s=new State();s.enabled=n.getBoolean("Enabled");s.ready=n.getBoolean("Ready");s.index=Math.floorMod(n.getInt("Index"),SCENES.size());return s;}
    }
    private static State state(ServerLevel world){return world.getDataStorage().computeIfAbsent(new SavedData.Factory<>(State::new,(nbt,registries)->State.read(nbt),null),KEY);}
    public static boolean enabled(ServerLevel world){return state(world).enabled;}
    public static boolean ready(ServerLevel world){return state(world).ready;}
    public static int index(ServerLevel world){return state(world).index;}
    public static BlockPos center(int index){return new BlockPos((index%6)*SPACING,Y,(index/6)*SPACING);}
    private static AABB bounds(int index){var c=center(index);return new AABB(Vec3.atLowerCornerOf(c.offset(-25,-5,-25)),Vec3.atLowerCornerOf(c.offset(26,38,26)));}
    public static void install(){
        var events=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        events.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event)->{
            if(event.getEntity() instanceof ServerPlayer player){var world=player.serverLevel();if(enabled(world)&&ready(world))enter(player,index(world));}
        });
        events.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event)->tick(event.getServer()));
        events.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event)->{BUILDING.remove(event.getServer());COOLDOWNS.clear();ACTIVE.clear();});
    }
    public static void beginBuild(ServerLevel world){
        if(!Boolean.getBoolean("royalespells.buildShowcase")||!(world.getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource))
            throw new IllegalStateException("Recording map generation requires the isolated flat-world builder");
        var st=state(world);st.enabled=true;st.ready=false;st.setDirty();
        var server=world.getServer();
        for(var rule:List.of(GameRules.RULE_DOMOBSPAWNING,GameRules.RULE_DAYLIGHT,GameRules.RULE_WEATHER_CYCLE,GameRules.RULE_DOFIRETICK,GameRules.RULE_MOBGRIEFING,GameRules.RULE_DO_PATROL_SPAWNING,GameRules.RULE_DO_TRADER_SPAWNING))world.getGameRules().getRule(rule).set(false,server);
        world.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true,server);world.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0,server);
        world.setDayTime(6000);world.setWeatherParameters(0,0,false,false);world.setDefaultSpawnPos(center(0).offset(0,2,-12),0);
        BUILDING.put(server,0);
    }
    private static void tick(MinecraftServer server){
        if(BUILDING.containsKey(server)){
            int i=BUILDING.get(server);var w=server.overworld();buildArena(w,i);
            System.out.println("ROYALE_MAP_BUILD "+(i+1)+"/"+SCENES.size());
            if(i+1==SCENES.size()){
                BUILDING.remove(server);var st=state(w);st.ready=true;st.setDirty();
                for(var p:server.getPlayerList().getPlayers())enter(p,0);
            }else BUILDING.put(server,i+1);
        }
        for(var p:server.getPlayerList().getPlayers()){
            var w=p.serverLevel();if(!enabled(w)||!ready(w))continue;
            for(var mob:w.getEntitiesOfClass(Mob.class,bounds(index(w)),e->e.getTags().contains(ACTOR)))mob.setRemainingFireTicks(0);
            if(p.getY()<Y-12){enter(p,index(w));continue;}
            // Staged friends wait for the first cast; actual spell and troop AI then run normally.
            for(var fx:w.getEntitiesOfClass(SpellEntity.class,bounds(index(w)),e->p.getUUID().equals(e.ownerId))){
                if(!ACTIVE.add(fx.getUUID()))continue;
                for(var mob:w.getEntitiesOfClass(Mob.class,bounds(index(w)),e->e.getTags().contains(FRIEND)))mob.setNoAi(false);
            }
        }
        if(server.getTickCount()%200==0)ACTIVE.clear();
    }
    public static void switchScene(ServerPlayer player,int direction){
        var world=player.serverLevel();if(!enabled(world)||!ready(world)){player.displayClientMessage(Component.literal("请在「皇室法术 · 录制片场」存档中使用场景控制。"),true);return;}
        long now=world.getGameTime();if(COOLDOWNS.getOrDefault(player.getUUID(),-100L)>now)return;
        COOLDOWNS.put(player.getUUID(),now+12);enter(player,Math.floorMod(index(world)+direction,SCENES.size()));
    }
    public static void enter(ServerPlayer player,int index){
        var world=player.serverLevel();if(!enabled(world)||!ready(world))return;
        cleanup(world,ShowcaseMap.index(world));
        var st=state(world);st.index=Math.floorMod(index,SCENES.size());st.setDirty();index=st.index;
        cleanup(world,index);resetSet(world,index);setupActors(world,player,index);
        var scene=SCENES.get(index);var c=center(index);
        player.setGameMode(GameType.CREATIVE);player.removeAllEffects();player.setRemainingFireTicks(0);
        double start=scene.spell().rolling()?(scene.spell()==Spell.THE_LOG?-8.5:-4.5):-12;
        double height=scene.spell().rolling()?Y:Y+2;
        player.teleportTo(world,c.getX()+.5,height,c.getZ()+start,0,scene.spell().rolling()?5:16.15f);
        player.getAbilities().flying=!scene.spell().rolling();player.onUpdateAbilities();
        player.setRespawnPosition(world.dimension(),c.offset(0,2,-12),0,true,false);
        var inv=player.getInventory();inv.clearContent();
        if(scene.spell()==Spell.MIRROR){inv.setItem(0,new ItemStack(RoyaleSpells.ITEMS.get(Spell.FIREBALL)));inv.setItem(1,new ItemStack(RoyaleSpells.ITEMS.get(Spell.MIRROR)));}
        else inv.setItem(0,new ItemStack(RoyaleSpells.ITEMS.get(scene.spell())));
        inv.setItem(7,new ItemStack(RoyaleSpells.PREVIOUS_SCENE));inv.setItem(8,new ItemStack(RoyaleSpells.NEXT_SCENE));inv.selected=0;
        player.containerMenu.broadcastChanges();player.connection.send(new ClientboundSetCarriedItemPacket(0));SpellEngine.resetForRecording(player);
        player.connection.send(new ClientboundSetTitlesAnimationPacket(5,35,10));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(String.format(Locale.ROOT,"%02d / %02d   ",index+1,SCENES.size())+scene.title()).withStyle(ChatFormatting.GOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(scene.hint()).withStyle(ChatFormatting.WHITE)));
    }
    private static void cleanup(ServerLevel w,int i){
        var box=bounds(i);EarthquakeDestruction.clearRegion(w,box);
        for(var e:w.getEntities((net.minecraft.world.entity.Entity)null,box,e->!(e instanceof net.minecraft.world.entity.player.Player)&&(e.getTags().contains(ACTOR)||e instanceof Summoned||e instanceof SpellEntity||e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.projectile.Projectile)))e.discard();
    }
    private static void put(ServerLevel w,BlockPos c,int x,int y,int z,Block b){w.setBlock(c.offset(x,y,z),b.defaultBlockState(),2);}
    private static void fill(ServerLevel w,BlockPos c,int x1,int y1,int z1,int x2,int y2,int z2,Block b){for(var p:BlockPos.betweenClosed(c.offset(x1,y1,z1),c.offset(x2,y2,z2)))w.setBlock(p,b.defaultBlockState(),2);}
    private static void text(ServerLevel w,Vec3 p,String message,float scale){
        var e=EntityType.TEXT_DISPLAY.create(w);var n=new CompoundTag();e.saveWithoutId(n);
        n.putString("text",Component.Serializer.toJson(Component.literal(message),w.registryAccess()));n.putString("billboard","center");n.putInt("line_width",360);n.putInt("background",0x90202b38);n.putByte("text_opacity",(byte)255);n.putBoolean("shadow",true);
        var transform=new ListTag();for(int i=0;i<16;i++)transform.add(FloatTag.valueOf(i==15?1:i==0||i==5||i==10?scale:0));n.put("transformation",transform);
        var brightness=new CompoundTag();brightness.putInt("block",15);brightness.putInt("sky",15);n.put("brightness",brightness);
        e.load(n);e.setPos(p);w.addFreshEntity(e);
    }
    private static void tower(ServerLevel w,BlockPos c,int x,int z,Block accent){
        fill(w,c,x-2,-1,z-2,x+2,8,z+2,Blocks.STONE_BRICKS);fill(w,c,x-2,8,z-2,x+2,9,z+2,accent);
        for(int dx=-2;dx<=2;dx+=2)for(int dz=-2;dz<=2;dz+=2)put(w,c,x+dx,10,z+dz,Blocks.STONE_BRICKS);
        fill(w,c,x,10,z,x,14,z,Blocks.OAK_FENCE);fill(w,c,x+1,12,z,x+4,14,z,accent);fill(w,c,x+1,12,z,x+1,14,z,Blocks.YELLOW_CONCRETE);
        put(w,c,x,6,z-3,Blocks.SEA_LANTERN);
    }
    private static void buildArena(ServerLevel w,int i){
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
        text(w,Vec3.atBottomCenterOf(c.offset(0,7,20)),String.format(Locale.ROOT,"%02d  /  %02d\n",i+1,SCENES.size())+s.title(),3f);
        text(w,Vec3.atBottomCenterOf(c.offset(0,4,-21)),"录制片场\n第 8 格：上一场景  ·  第 9 格：下一场景\n潜行 + 使用控制物品：重置当前场景",.7f);
        resetSet(w,i);
    }
    private static void resetSet(ServerLevel w,int i){
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
    private static void tree(ServerLevel w,BlockPos c,int x,int z,boolean lush){
        fill(w,c,x,0,z,x,6,z,Blocks.OAK_LOG);
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int y=4;y<=7;y++)if(Math.abs(dx)+Math.abs(dz)<=(y==7?1:3))w.setBlock(c.offset(x+dx,y,z+dz),(lush?Blocks.AZALEA_LEAVES:Blocks.OAK_LEAVES).defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),2);
    }
    private static Mob target(ServerLevel w,BlockPos c,EntityType<? extends Mob> type,double x,double z,float hp){
        var mob=type.create(w);mob.moveTo(c.getX()+.5+x,Y,c.getZ()+.5+z,180,0);mob.setYBodyRot(180);mob.setYHeadRot(180);mob.setNoAi(true);mob.setPersistenceRequired();mob.setSilent(true);mob.setOnGround(true);mob.addTag(ACTOR);
        var max=mob.getAttribute(Attributes.MAX_HEALTH);if(max!=null)max.setBaseValue(hp);mob.setHealth(hp);
        w.addFreshEntity(mob);return mob;
    }
    private static void friend(ServerLevel w,ServerPlayer p,BlockPos c,String kind,double x,double z){
        var mob=SpellEngine.summon(w,p.getUUID(),new Vec3(c.getX()+.5+x,Y,c.getZ()+.5+z),kind,false);
        if(mob==null)throw new IllegalStateException("Recording friend spawn blocked");
        ((Summoned)mob).setup(p.getUUID(),72000,false);mob.setNoAi(true);mob.setYRot(0);mob.setYHeadRot(0);mob.setYBodyRot(0);mob.addTag(ACTOR);mob.addTag(FRIEND);
    }
    private static void setupActors(ServerLevel w,ServerPlayer player,int i){
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
