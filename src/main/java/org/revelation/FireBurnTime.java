package org.revelation;

import java.util.Properties;
import java.util.logging.Logger;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

public class FireBurnTime implements WurmServerMod, Configurable, PreInitable {
    private static final Logger logger = Logger.getLogger(FireBurnTime.class.getName());

    public int TargetTemperature = 5000;

    public void configure (Properties properties){
        TargetTemperature = Integer.parseInt(properties.getProperty("TargetTemperature"));

    }
    @Override
    public String getVersion() {
        return "3.0";
    }
    @Override
    public void preInit() {
        logger.info("Initialising FireBurnTime 3.0");

        try {
            CtClass ctClass = HookManager.getInstance().getClassPool().get("com.wurmonline.server.behaviours.FireBehaviour");
            CtClass[] parameters = new CtClass[] {
                    HookManager.getInstance().getClassPool().get("com.wurmonline.server.behaviours.Action"),
                    HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature"),
                    HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"),
                    CtPrimitiveType.shortType,
                    CtPrimitiveType.floatType
            };
            CtMethod ctMethod = ctClass.getMethod("action", Descriptor.ofMethod(CtPrimitiveType.booleanType, parameters));
            ctMethod.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall methodCall) throws CannotCompileException {
                    if (methodCall.getMethodName().equals("sendEnchantmentStrings")) {
                        ctMethod.insertAt(methodCall.getLineNumber(),
                                "{" +
                                        "float coolingSpeed = 1.0f;"+
                                        "com.wurmonline.server.zones.VolaTile forgeTile = com.wurmonline.server.zones.Zones.getTileOrNull(target.getTilePos(), target.isOnSurface());"+
                                        "if (forgeTile != null && forgeTile.getStructure() != null)"+
                                        "    coolingSpeed *= 0.75f;"+
                                        "else if (com.wurmonline.server.Server.getWeather().getRain() > 0.2f)"+
                                        "    coolingSpeed *= 2f;"+

                                        "if (target.getRarity() > 0)"+
                                        "    coolingSpeed *= Math.pow(0.8999999761581421, (double)target.getRarity());"+

                                        "float decreaseTemperature = coolingSpeed * Math.max(1f, 11f - Math.max(1f, 20f * Math.max(30f, target.getCurrentQualityLevel()) / 200f));"+
                                        "int secondsRemaining = Math.round(Math.max(0, target.getTemperature() - " + TargetTemperature + ") / decreaseTemperature);"+

                                        "if (target.getTemperature() > 999) performer.getCommunicator().sendNormalServerMessage(\"The forge will need to be re-lit in about \" + secondsRemaining / 60 + \" minutes and \" + secondsRemaining % 60 + \" seconds.\");"+
                                        "else performer.getCommunicator().sendNormalServerMessage(\"The forge must be re-lit to continue.\");"+
                                        "}"
                        );
                    }
                }
            });
        } catch (CannotCompileException | NotFoundException e) {
            logger.severe("FireBurnTime 3.0 could not be applied." + e);
        }
    }
}