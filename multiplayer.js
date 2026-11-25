/* Multiplayer para los tres juegos de mesa. Requiere GamesOnline en index.html. */
(function(){
  const esc=s=>String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
  window.installMultiplayer=function(cfg){
    const box=document.createElement("section");
    box.className="card"; box.style.cssText="width:min(100%,760px);margin:0 auto 14px;display:flex;gap:8px;align-items:center;flex-wrap:wrap";
    box.innerHTML=`<b>🌐 Multijugador</b><span id="mpInfo" style="color:#98a0b3;flex:1">Juega online con usuarios registrados</span><button id="mpCreate" class="pri">Crear sala</button><input id="mpCode" maxlength="6" placeholder="Código" aria-label="Código de sala" style="width:110px;margin:0;text-transform:uppercase"><button id="mpJoin">Unirse</button><button id="mpLeave" hidden>Salir de sala</button>`;
    document.body.insertBefore(box,document.querySelector(".game"));
    const $=id=>box.querySelector("#"+id);
    const M={active:false,room:null,seat:-1,version:0,players:[],unwatch:null,busy:false,
      isMyTurn(){return this.active&&this.room&&this.players.length===this.room.capacity&&this.players[this.getTurn()]===window.GamesOnline.identity()?.id},
      getTurn:cfg.getTurn,
      async publish(){
        if(!this.active||this.busy||!this.isMyTurn())return;
        this.busy=true;
        try{
          const res=await window.GamesOnline.move(this.room.id,this.version,cfg.getState(),cfg.getTurn(),cfg.isOver?.()?"finished":"playing");
          this.version=res.version??this.version+1;
        }catch(e){$("mpInfo").textContent="No se pudo sincronizar: "+e.message}
        finally{this.busy=false}
      },
      async attach(room,seat){
        this.room=room;this.seat=seat;this.players=room.player_ids||[];this.version=room.version||0;this.active=true;
        if(this.unwatch)this.unwatch();
        this.unwatch=await window.GamesOnline.watch(room.id,row=>{
          if(row.version<=this.version)return;
          this.version=row.version;this.players=row.player_ids||this.players;
          if(row.state)cfg.setState(row.state);
          this.paint();
        });
        this.paint();
      },
      paint(){
        $("mpInfo").textContent=this.active
          ?`Sala ${this.room.code} · ${this.players.length}/${this.room.capacity} jugadores · ${this.players.length<this.room.capacity?"Esperando jugadores":this.isMyTurn()?"Tu turno":"Esperando turno rival"}`
          :"Juega online con usuarios registrados";
        $("mpCreate").hidden=this.active;$("mpJoin").hidden=this.active;$("mpCode").hidden=this.active;$("mpLeave").hidden=!this.active;
        if(this.active)cfg.onMode?.(this.seat,this.players.length);
      },
      async create(){
        try{
          if(!window.GamesOnline?.identity())throw Error("Inicia sesión desde la página principal.");
          const room=await window.GamesOnline.create(cfg.game,cfg.capacity(),cfg.getState());
          await this.attach(room,0);$("mpInfo").textContent=`Sala ${room.code} creada. Comparte el código.`;
        }catch(e){$("mpInfo").textContent=e.message}
      },
      async join(){
        try{
          if(!window.GamesOnline?.identity())throw Error("Inicia sesión desde la página principal.");
          const room=await window.GamesOnline.join($("mpCode").value);
          const seat=room.player_ids.findIndex(id=>id===window.GamesOnline.identity().id);
          if(seat<0)throw Error("No se pudo asignar tu plaza.");
          await this.attach(room,seat);
          cfg.setState(room.state);
        }catch(e){$("mpInfo").textContent=e.message}
      },
      leave(){
        if(this.unwatch)this.unwatch();this.unwatch=null;this.active=false;this.room=null;this.seat=-1;this.players=[];this.paint();cfg.onLeave?.();
      }
    };
    $("mpCreate").onclick=()=>M.create();$("mpJoin").onclick=()=>M.join();$("mpLeave").onclick=()=>M.leave();
    window.MP=M;return M;
  };
})();
