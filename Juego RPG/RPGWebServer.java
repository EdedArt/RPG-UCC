import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RPGWebServer {

    private static HeroCharacter hero;

    public static void main(String[] args) throws IOException {
        hero = new BaseHero("Cyber Guerrero", "🥷", 150, 20, 25, 10);

        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/", new MainMenuHandler());
        server.createContext("/equip", new DecoratorHandler());
        server.createContext("/combat", new CombatHandler());
        server.createContext("/reset", new ResetHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("Servidor Web HTTP RPG Dinámico iniciado en http://localhost:8081");
        System.out.println("Renderizando interfaz animada desde Java...");
    }

    // ============================================
    // MANEJADORES DE RUTAS WEB
    // ============================================
    
    static class MainMenuHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String ui = renderizarMenuPrincipal(hero);
            byte[] bytes = ui.getBytes(StandardCharsets.UTF_8);
            t.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            t.sendResponseHeaders(200, bytes.length);
            OutputStream os = t.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class DecoratorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
                InputStream is = t.getRequestBody();
                byte[] requestBytes = new byte[is.available()];
                is.read(requestBytes);
                String body = new String(requestBytes, StandardCharsets.UTF_8).replace("item=", "");

                // POLIMORFISMO DECORATOR
                if (body.equals("Espada")) hero = new WeaponDecorator(hero, "Plasma Blade", 30, 0);
                if (body.equals("Armadura")) hero = new ArmorDecorator(hero, "Exo-Armadura", 40, 20);
                if (body.equals("Poder")) hero = new PowerDecorator(hero, "Rayo Eldritch", 40, 10);
                if (body.equals("Buff")) hero = new BuffDecorator(hero, "Poción Adrenalina", 15);

                t.getResponseHeaders().add("Location", "/");
                t.sendResponseHeaders(302, -1);
            }
        }
    }

    static class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
                InputStream is = t.getRequestBody();
                byte[] requestBytes = new byte[is.available()];
                is.read(requestBytes);
                String body = new String(requestBytes, StandardCharsets.UTF_8).replace("clase=", "");
                
                if (body.equals("Guerrero")) hero = new BaseHero("Cyber Guerrero", "🥷", 150, 20, 25, 10);
                else if (body.equals("Mago")) hero = new BaseHero("Mago del Vacío", "🧙‍♂️", 100, 35, 10, 15);
                else if (body.equals("Asesino")) hero = new BaseHero("Asesino Neón", "🗡️", 110, 30, 12, 30);
                else hero = new BaseHero("Cyber Guerrero", "🥷", 150, 20, 25, 10);

                t.getResponseHeaders().add("Location", "/");
                t.sendResponseHeaders(302, -1);
            }
        }
    }

    // ============================================
    // SISTEMA DE COMBATE ANIMADO
    // ============================================

    static class CombatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            int bossMaxHp = 450;
            int bossHp = bossMaxHp;
            int bossDmgBase = 45;
            
            int heroMaxHp = hero.getHp();
            int heroHp = heroMaxHp;
            
            StringBuilder jsonArray = new StringBuilder("[");
            
            // Simulación de turnos generada por el backend Java
            while(heroHp > 0 && bossHp > 0) {
                // Turno Héroe
                int dodge = Math.max(0, 15 - hero.getSpeed());
                if(Math.random()*100 > dodge) {
                    int dmgDealt = hero.getDamage() + (int)(Math.random() * 15);
                    bossHp -= dmgDealt;
                    jsonArray.append(String.format("{\"attacker\":\"hero\", \"dmg\":%d, \"heroHp\":%d, \"bossHp\":%d},", dmgDealt, Math.max(0,heroHp), Math.max(0,bossHp)));
                } else {
                    jsonArray.append(String.format("{\"attacker\":\"hero_miss\", \"dmg\":0, \"heroHp\":%d, \"bossHp\":%d},", Math.max(0,heroHp), Math.max(0,bossHp)));
                }
                
                if (bossHp <= 0) break;
                
                // Turno Boss
                int damageTaken = Math.max(5, bossDmgBase - hero.getDefense() + (int)(Math.random() * 10));
                heroHp -= damageTaken;
                jsonArray.append(String.format("{\"attacker\":\"boss\", \"dmg\":%d, \"heroHp\":%d, \"bossHp\":%d},", damageTaken, Math.max(0,heroHp), Math.max(0,bossHp)));
            }
            if(jsonArray.charAt(jsonArray.length()-1) == ',') {
                jsonArray.deleteCharAt(jsonArray.length()-1);
            }
            jsonArray.append("]");

            String ui = renderizarArenaCombate(hero, jsonArray.toString(), bossMaxHp, heroMaxHp);
            byte[] bytes = ui.getBytes(StandardCharsets.UTF_8);
            t.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            t.sendResponseHeaders(200, bytes.length);
            OutputStream os = t.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }


    // ============================================
    // CONSTRUCTORES DE HTML NATIVOS DE JAVA
    // ============================================

    private static String renderizarMenuPrincipal(HeroCharacter current) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'><title>RPG EDGAR</title>");
        html.append("<style>");
        html.append("body { background: #0b0c10; color: #c5c6c7; font-family: 'Segoe UI', Tahoma, Verdana, sans-serif; margin: 0; padding: 20px; overflow-x: hidden; }");
        html.append("h1, h2, h3 { color: #66fcf1; text-align: center; text-transform: uppercase; letter-spacing: 2px; text-shadow: 0 0 10px rgba(102,252,241,0.5); }");
        html.append(".container { display: flex; justify-content: center; flex-wrap: wrap; max-width: 1200px; margin: 0 auto; gap: 30px; }");
        html.append(".card { background: #1f2833; border-radius: 15px; padding: 25px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); border-top: 4px solid #45a29e; flex: 1; min-width: 300px; max-width: 500px; transition: transform 0.3s; position: relative; overflow: hidden; }");
        html.append(".card:hover { transform: translateY(-5px); box-shadow: 0 15px 40px rgba(102, 252, 241, 0.2); }");
        
        html.append(".avatar { font-size: 100px; text-align: center; margin: 10px 0; animation: float 3s ease-in-out infinite; filter: drop-shadow(0 0 20px rgba(102,252,241,0.4)); }");
        html.append("@keyframes float { 0% { transform: translateY(0px); } 50% { transform: translateY(-15px); } 100% { transform: translateY(0px); } }");
        
        html.append(".btn { display: block; width: 100%; margin: 12px 0; padding: 15px; background: rgba(31, 40, 51, 0.8); border: 2px solid #45a29e; color: #66fcf1; font-size: 16px; font-weight: bold; border-radius: 8px; cursor: pointer; transition: 0.3s; position: relative; overflow: hidden; }");
        html.append(".btn:hover { background: #45a29e; color: #0b0c10; box-shadow: 0 0 20px #45a29e; transform: scale(1.02); }");
        html.append(".btn-combat { background: #900; border-color: #f00; color: #fff; text-shadow: 1px 1px 5px #000; font-size: 20px; letter-spacing: 2px; }");
        html.append(".btn-combat:hover { background: #f00; box-shadow: 0 0 30px #f00; color: #fff; }");
        
        html.append(".stat-label { display: flex; justify-content: space-between; font-weight: bold; margin-bottom: 2px; margin-top: 10px; font-size: 14px;}");
        html.append(".stat-bar { background: #0b0c10; height: 15px; border-radius: 10px; margin-bottom: 5px; overflow: hidden; border: 1px solid #333; }");
        html.append(".fill { height: 100%; border-radius: 10px; transition: width 0.8s cubic-bezier(0.34, 1.56, 0.64, 1); }");
        html.append(".char-select { display: flex; justify-content: space-between; gap: 10px; }");
        html.append(".char-btn { flex: 1; padding: 12px; font-weight:bold; font-size:14px; background: rgba(0,0,0,0.4); border: 1px solid #45a29e; color: #66fcf1; cursor: pointer; transition: 0.3s; border-radius: 5px; }");
        html.append(".char-btn:hover { background: #45a29e; color: #000; transform: translateY(-3px); box-shadow: 0 5px 15px rgba(69, 162, 158, 0.4); }");
        html.append("</style></head><body>");
        
        html.append("<h1>🔥 RPG EDGAR (Patrón Decorator) 🔥</h1>");
        html.append("<div class='container'>");
        
        // Panel del Héroe
        html.append("<div class='card'>");
        html.append("<h2>ESTADO DEL HÉROE</h2>");
        
        String avatar = current.getAvatar();
        
        html.append("<div class='avatar'>").append(avatar).append("</div>");
        html.append("<h3>▶ ").append(current.getName()).append(" ◀</h3>");
        html.append("<p style='text-align:center; color:#c5c6c7; font-size:13px; line-height: 1.6; margin-bottom: 20px;'><i>").append(current.getDescription()).append("</i></p>");
        
        int hpPct = Math.min(100, (int)((current.getHp() / 300.0) * 100));
        int dmgPct = Math.min(100, (int)((current.getDamage() / 150.0) * 100));
        int defPct = Math.min(100, (int)((current.getDefense() / 100.0) * 100));
        int spdPct = Math.min(100, (int)((current.getSpeed() / 60.0) * 100));
        
        html.append("<div class='stat-label'><span>❤️ Salud (HP)</span><span>").append(current.getHp()).append("</span></div>");
        html.append("<div class='stat-bar'><div class='fill' style='width:").append(hpPct).append("%; background: linear-gradient(90deg, #f05454, #ff8c8c); box-shadow: 0 0 10px #f05454;'></div></div>");
        
        html.append("<div class='stat-label'><span>⚔️ Poder Ataque (ATQ)</span><span>").append(current.getDamage()).append("</span></div>");
        html.append("<div class='stat-bar'><div class='fill' style='width:").append(dmgPct).append("%; background: linear-gradient(90deg, #fca311, #ffc971); box-shadow: 0 0 10px #fca311;'></div></div>");
        
        html.append("<div class='stat-label'><span>🛡️ Defensa (DEF)</span><span>").append(current.getDefense()).append("</span></div>");
        html.append("<div class='stat-bar'><div class='fill' style='width:").append(defPct).append("%; background: linear-gradient(90deg, #45a29e, #84dcc6); box-shadow: 0 0 10px #45a29e;'></div></div>");
        
        html.append("<div class='stat-label'><span>⚡ Velocidad (VEL)</span><span>").append(current.getSpeed()).append("</span></div>");
        html.append("<div class='stat-bar'><div class='fill' style='width:").append(spdPct).append("%; background: linear-gradient(90deg, #b6e9df, #e0fbf5); box-shadow: 0 0 10px #b6e9df;'></div></div>");
        
        html.append("</div>");
        
        // Panel de Equipamiento
        html.append("<div class='card'>");
        html.append("<h2>🛠️ ARMAMENTO Y MAGIA</h2>");
        
        html.append("<form method='POST' action='/reset'>");
        html.append("<p style='text-align:center; margin:10px 0; color:#c5c6c7; font-size: 14px;'>CAMBIAR PERSONAJE BASE:</p>");
        html.append("<div class='char-select'>");
        html.append("<button class='char-btn' name='clase' value='Guerrero'>🛡️ GUERRERO</button>");
        html.append("<button class='char-btn' name='clase' value='Mago'>🧙‍♂️ MAGO</button>");
        html.append("<button class='char-btn' name='clase' value='Asesino'>🗡️ ASESINO</button>");
        html.append("</div>");
        html.append("</form><hr style='border: 1px solid rgba(255,255,255,0.1); margin: 25px 0;'>");
        
        html.append("<form action='/equip' method='POST'>");
        html.append("<button class='btn' name='item' value='Espada'>🗡️ Equipar: Plasma Blade (+ATQ)</button>");
        html.append("<button class='btn' name='item' value='Armadura'>⚙️ Equipar: Exo-Armadura (+DEF)</button>");
        html.append("<button class='btn' name='item' value='Poder'>☄️ Aprender: Rayo Eldritch (+ATQ, +HP)</button>");
        html.append("<button class='btn' name='item' value='Buff'>🧪 Beber: Poción Adrenalina (+VEL)</button>");
        html.append("</form>");
        
        html.append("<form action='/combat' method='POST' style='margin-top:20px;'><button class='btn btn-combat'>☠️ ENTRAR A LA ARENA VS JEFE ☠️</button></form>");
        html.append("</div>");
        
        html.append("</div></body></html>");
        return html.toString();
    }

    // ============================================
    // RENDERIZADOR DE ARENA DE COMBATE (ANIMADA)
    // ============================================
    private static String renderizarArenaCombate(HeroCharacter current, String dataJSON, int bossMax, int heroMax) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'><title>Arena de Combate</title>");
        html.append("<style>");
        html.append("body { background: radial-gradient(circle at center, #2e1114 0%, #000 100%); color: white; margin: 0; font-family: 'Segoe UI', Tahoma, Verdana, sans-serif; overflow: hidden; height: 100vh; }");
        html.append(".hud { position: absolute; top: 30px; width: 100%; display: flex; justify-content: space-between; padding: 0 100px; box-sizing: border-box; z-index: 10; }");
        html.append(".health-box { width: 350px; background: rgba(0,0,0,0.8); padding: 15px; border-radius: 10px; border: 2px solid #66fcf1; box-shadow: 0 0 20px rgba(102,252,241,0.3); }");
        html.append(".boss-box { border-color: #f05454; box-shadow: 0 0 20px rgba(240,84,84,0.3); text-align: right; }");
        html.append(".health-bar { height: 25px; background: #333; border-radius: 5px; overflow: hidden; margin-top: 10px; }");
        html.append(".health-fill { height: 100%; width: 100%; transition: width 0.3s; }");
        html.append(".hero-fill { background: linear-gradient(90deg, #45a29e, #66fcf1); }");
        html.append(".boss-fill { background: linear-gradient(90deg, #f00, #f05454); }");
        html.append(".name { font-size: 28px; font-weight: bold; text-transform: uppercase; color: #fff; text-shadow: 0 0 10px #0ff; margin:0;}");
        html.append(".boss-box .name { text-shadow: 0 0 10px #f00; }");
        
        html.append(".sprites { position: absolute; bottom: 15%; display: flex; justify-content: space-between; width: 60%; left: 20%; align-items: flex-end; }");
        html.append(".sprite { font-size: 180px; position: relative; transition: transform 0.2s; filter: drop-shadow(0 0 20px rgba(102,252,241,0.6)); }");
        html.append(".boss-sprite { font-size: 250px; font-family: sans-serif; filter: drop-shadow(0 0 30px rgba(255,0,0,0.6)); transform: scaleX(-1); }");
        
        html.append(".attack-hero { animation: heroStrike 0.5s ease-in-out; }");
        html.append(".attack-boss { animation: bossStrike 0.5s ease-in-out; }");
        html.append(".take-hit { animation: shake 0.3s; filter: brightness(2) drop-shadow(0 0 40px red) !important; }");
        html.append(".miss { animation: fadeUp 1s forwards; filter: none !important; opacity:0; }");
        
        html.append("@keyframes heroStrike { 0% { transform: translateX(0); } 50% { transform: translateX(350px) rotate(20deg); } 100% { transform: translateX(0); } }");
        html.append("@keyframes bossStrike { 0% { transform: scaleX(-1) translateX(0); } 50% { transform: scaleX(-1) translateX(-350px) rotate(-20deg); } 100% { transform: scaleX(-1) translateX(0); } }");
        html.append("@keyframes shake { 0%, 100%{ transform: translateX(0); } 25%{ transform: translateX(-15px); } 75%{ transform: translateX(15px); } }");
        html.append("@keyframes floatUp { 0% { opacity: 1; transform: translateY(0) scale(1.5); } 100% { opacity: 0; transform: translateY(-150px) scale(0.5); } }");
        html.append("@keyframes fadeUp { 0% { opacity: 1; transform: translateY(0); } 100% { opacity: 0; transform: translateY(-50px); } }");

        html.append(".damage-text { position: absolute; font-size: 60px; font-weight: bold; color: yellow; text-shadow: 3px 3px 0 #000, -3px -3px 0 #000, 3px -3px 0 #000, -3px 3px 0 #000; opacity: 0; animation: floatUp 1s ease-out forwards; pointer-events: none; z-index: 100; }");
        
        html.append(".overlay-result { display: none; position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.85); z-index: 200; flex-direction: column; justify-content: center; align-items: center; }");
        html.append(".overlay-result h1 { font-size: 100px; margin: 0; text-transform: uppercase; letter-spacing: 5px; }");
        html.append(".win { color: #0f0; text-shadow: 0 0 40px #0f0; }");
        html.append(".lose { color: #f00; text-shadow: 0 0 40px #f00; }");
        html.append("a.btn-return { margin-top: 40px; padding: 20px 50px; font-size: 24px; background: #fff; color: #000; text-decoration: none; font-weight: bold; border-radius: 10px; transition: 0.3s; box-shadow: 0 0 20px #fff; }");
        html.append("a.btn-return:hover { transform: scale(1.1); background: #0ff; box-shadow: 0 0 40px #0ff; }");
        
        html.append("</style></head><body>");

        // HUD
        html.append("<div class='hud'>");
        html.append("<div class='health-box'>");
        html.append("<p class='name'>").append(current.getName()).append("</p>");
        html.append("<div class='health-bar'><div class='health-fill hero-fill' id='heroFill'></div></div>");
        html.append("<p style='margin:5px 0 0 0; font-weight:bold;' id='heroTxt'>").append(heroMax).append("/").append(heroMax).append(" HP</p>");
        html.append("</div>");
        
        html.append("<div class='health-box boss-box'>");
        html.append("<p class='name'>EL OBSERVADOR</p>");
        html.append("<div class='health-bar'><div class='health-fill boss-fill' id='bossFill'></div></div>");
        html.append("<p style='margin:5px 0 0 0; font-weight:bold;' id='bossTxt'>").append(bossMax).append("/").append(bossMax).append(" HP</p>");
        html.append("</div>");
        html.append("</div>");
        
        // SPRITES
        String heroSprite = current.getAvatar();

        html.append("<div class='sprites'>");
        html.append("<div class='sprite' id='heroSprite'>").append(heroSprite).append("</div>");
        html.append("<div class='sprite boss-sprite' id='bossSprite'>👾</div>");
        html.append("</div>");
        
        // RESULT SCREEN
        html.append("<div class='overlay-result' id='resultScreen'>");
        html.append("<h1 id='resultTitle'></h1>");
        html.append("<a href='/' class='btn-return'>VOLVER A RPG EDGAR</a>");
        html.append("</div>");

        // JAVASCRIPT: Motor de animacion inyectado desde Java
        html.append("<script>");
        html.append("const heroSprite = document.getElementById('heroSprite');");
        html.append("const bossSprite = document.getElementById('bossSprite');");
        html.append("const heroFill = document.getElementById('heroFill');");
        html.append("const bossFill = document.getElementById('bossFill');");
        html.append("const heroTxt = document.getElementById('heroTxt');");
        html.append("const bossTxt = document.getElementById('bossTxt');");
        
        html.append("const maxHero = ").append(heroMax).append(";");
        html.append("const maxBoss = ").append(bossMax).append(";");
        html.append("const turns = ").append(dataJSON).append(";");
        
        html.append("let idx = 0;");
        html.append("function playTurn() {");
        html.append("  if (idx >= turns.length){ showResult(); return; }");
        html.append("  let t = turns[idx];");
        html.append("  let isHero = (t.attacker === 'hero' || t.attacker === 'hero_miss');");
        html.append("  let attacker = isHero ? heroSprite : bossSprite;");
        html.append("  let target = isHero ? bossSprite : heroSprite;");
        
        html.append("  attacker.classList.add(isHero ? 'attack-hero' : 'attack-boss');");
        
        html.append("  setTimeout(() => {");
        html.append("    if(t.attacker === 'hero_miss'){");
        html.append("       let m = document.createElement('div'); m.className = 'damage-text miss'; m.innerText='FALLÓ!'; target.appendChild(m);");
        html.append("       setTimeout(()=>m.remove(), 1000);");
        html.append("    } else {");
        html.append("       target.classList.add('take-hit');");
        html.append("       let d = document.createElement('div'); d.className = 'damage-text'; d.innerText='-'+t.dmg; d.style.left=(Math.random()*40+30)+'%'; d.style.top=(Math.random()*40+10)+'%'; target.appendChild(d);");
        html.append("       if(isHero){ bossFill.style.width=(t.bossHp/maxBoss)*100+'%'; bossTxt.innerText=t.bossHp+'/'+maxBoss+' HP'; }");
        html.append("       else { heroFill.style.width=(t.heroHp/maxHero)*100+'%'; heroTxt.innerText=t.heroHp+'/'+maxHero+' HP'; }");
        html.append("       setTimeout(()=>d.remove(), 1000);");
        html.append("    }");
        
        html.append("    setTimeout(() => { attacker.classList.remove('attack-hero', 'attack-boss'); target.classList.remove('take-hit', 'miss'); idx++; setTimeout(playTurn, 600); }, 400);");
        html.append("  }, 250);");
        html.append("}");

        html.append("function showResult() {");
        html.append("  let res = document.getElementById('resultScreen');");
        html.append("  let t = document.getElementById('resultTitle');");
        html.append("  let last = turns[turns.length-1];");
        html.append("  if(!last)return;");
        html.append("  res.style.display='flex';");
        html.append("  if(last.heroHp > 0){ res.classList.add('win'); t.innerText='¡VICTORIA HEROICA!'; }");
        html.append("  else{ res.classList.add('lose'); t.innerText='¡HAS SIDO DERROTADO!'; }");
        html.append("}");

        html.append("setTimeout(playTurn, 1000);"); // Inicio retardo
        html.append("</script>");
        
        html.append("</body></html>");
        return html.toString();
    }
}
