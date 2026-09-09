package dev.mineclash.zappies;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.block.state.BlockState;
import org.liziyowo.mineclash.CREntities;
import org.liziyowo.mineclash.entity.mob.Zappies;
import java.util.*;
/** Reserve all three one-block bodies before inserting any entity or consuming an egg. */
public final class ZappySquad {
    public static List<Zappies> spawn(ServerLevel w,Vec3 center,float yaw,UUID owner,int level){
        Vec3 forward=new Vec3(-Math.sin(Math.toRadians(yaw)),0,Math.cos(Math.toRadians(yaw)));
        Vec3 right=new Vec3(forward.z,0,-forward.x);List<Zappies> planned=new ArrayList<>();UUID squad=UUID.randomUUID();
        for(int i=0;i<3;i++){
            Vec3 point=center.add(forward.scale(i==0?.65:-.65)).add(right.scale(i==1?-1.15:i==2?1.15:0));
            Vec3 ground=ground(w,point);if(ground==null)return List.of();
            var z=CREntities.ZAPPIES.get().create(w);if(z==null)return List.of();z.moveTo(ground.x,ground.y,ground.z,yaw,0);z.setYBodyRot(yaw);z.setYHeadRot(yaw);
            if(!w.getWorldBorder().isWithinBounds(z.getBoundingBox())||!w.noCollision(z,z.getBoundingBox())||!w.getEntitiesOfClass(LivingEntity.class,z.getBoundingBox()).isEmpty()
                ||planned.stream().anyMatch(e->e.getBoundingBox().intersects(z.getBoundingBox())))return List.of();
            ZappyBrain.of(z).configure(level,owner,squad,i,20+i*2);planned.add(z);
        }
        for(var z:planned)if(!w.addFreshEntity(z)){planned.forEach(e->{if(e.isAddedToLevel())e.discard();});return List.of();}
        return List.copyOf(planned);
    }
    private static Vec3 ground(ServerLevel w,Vec3 p){
        BlockPos b=BlockPos.containing(p);
        for(int dy=1;dy>=-2;dy--){BlockPos ground=b.offset(0,dy,0);BlockState state=w.getBlockState(ground);
            var shape=state.getCollisionShape(w,ground);if(shape.isEmpty())continue;
            double top=shape.max(net.minecraft.core.Direction.Axis.Y);Vec3 result=new Vec3(p.x,ground.getY()+top,p.z);
            if(w.noCollision(new AABB(result.x-.5,result.y,result.z-.5,result.x+.5,result.y+1.3,result.z+.5)))return result;}
        return null;
    }
}
