/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
 (function(){

 	var prodAndes= angular.module('ProdAndes',[]);

 	prodAndes.directive('toolbar', function(){
 		return{
 			restrict:'E',
 			templateUrl: 'partials/toolbar.html',
 			controller:function(){
 				this.tab=0;
                this.selectTab=function(setTab){
                    this.tab=setTab;
                };
                this.isSelected=function(tabParam){
                    return this.tab===tabParam;
                };
            },
            controllerAs:'toolbar'
        };
    });

 	prodAndes.directive('navegacion', function(){
 		return{
 			restrict:'E',
 			templateUrl: 'partials/navegacion.html',
 			controller:function(){
 				
 			},
 			controllerAs:'navegacion'
 		};
 	});


 	prodAndes.directive('registrarPedidoForm', function(){
        return{
            restrict:'E',
            templateUrl:'partials/registrar-pedido-form.html',
            controller: ['$http',function($http){
                var self=this;
                self.pedido={};
                self.id_pedido=null;

                this.hayIdPedido=function(){
                    return this.id_pedido!==null;
                };

                this.addPedido=function(pedidoParam){

                    self.id_pedido=null;
                    self.pedido = pedidoParam,
                    console.log('Form pedido '+JSON.stringify(self.pedido));
                    $http.post('http://localhost:8080/backend/Servicios/registrarPedido'
                    	, self.pedido).success(function(data){

                            console.log('Data '+JSON.stringify(data));
                            alert(""+data.Respuesta);
                            self.id_pedido=data.id_pedido;
                            console.log('Id pedido '+self.id_pedido);
                            self.pedido={};
                        });
                    };

                    this.cancelPedido=function(){


                        self.pedido = {};
                        self.pedido.id_pedido = self.id_pedido;
                        console.log('Form pedido cancelar '+JSON.stringify(self.pedido));
                        $http.post('http://localhost:8080/backend/Servicios/cancelarPedido'
                            , self.pedido).success(function(data){

                                console.log('Data '+JSON.stringify(data));
                                alert("Se ha cancelado su pedido");

                            });
                        };



                    }],
                    controllerAs:'registrarPedidoCtrl'
                };
            });



prodAndes.directive('registrarEntregaPedidoForm', function(){
    return{
        restrict:'E',
        templateUrl:'partials/registrar-entrega-pedido-form.html',
        controller: ['$http',function($http){
            var self=this;
            self.pedido={};
            this.addPedido=function(pedidoParam){

             self.pedido = pedidoParam,
             console.log('Form pedido '+JSON.stringify(self.pedido));
             $http.post('http://localhost:8080/backend/Servicios/registrarEntregaPedidoProductosCliente'
                 , self.pedido).success(function(data){
                     alert("Se ha registrado la entrega del pedido");
                     self.pedido={};
                 });
             };
         }],
         controllerAs:'registrarEntregaPedidoCtrl'
     };
 });

prodAndes.directive('toolbarConsultaProducto', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/toolbar-consulta-producto.html',
        controller:function(){
            this.tab=0;
            this.selectTab=function(setTab){
                this.tab=setTab;
            };
            this.isSelected=function(tabParam){
                return this.tab===tabParam;
            };
        },
        controllerAs:'toolbarConsultaProductoCtrl'
    };
});

prodAndes.directive('toolbarConsultaClientes', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/toolbar-consulta-clientes.html',
        controller:function(){
            this.tab=0;
            this.selectTab=function(setTab){
                this.tab=setTab;
            };
            this.isSelected=function(tabParam){
                return this.tab===tabParam;
            };
        },
        controllerAs:'toolbarConsultaClientesCtrl'
    };
});

prodAndes.directive('toolbarConsultaRf10', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/toolbar-consulta-rf10.html',
        controller:function(){
            this.tab=0;
            this.selectTab=function(setTab){
                this.tab=setTab;
            };
            this.isSelected=function(tabParam){
                return this.tab===tabParam;
            };
        },
        controllerAs:'toolbarConsultaRf10Ctrl'
    };
});

prodAndes.directive('toolbarConsultaRf11', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/toolbar-consulta-rf11.html',
        controller:function(){
            this.tab=0;
            this.selectTab=function(setTab){
                this.tab=setTab;
            };
            this.isSelected=function(tabParam){
                return this.tab===tabParam;
            };
        },
        controllerAs:'toolbarConsultaRf11Ctrl'
    };
});

prodAndes.directive('toolbarConsultaPedidos', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/toolbar-consulta-pedidos.html',
        controller:function(){
            this.tab=0;
            this.selectTab=function(setTab){
                console.log("Select Tab Pedidos");
                this.tab=setTab;
            };
            this.isSelected=function(tabParam){
                return this.tab===tabParam;
            };
        },
        controllerAs:'toolbarConsultaPedidosCtrl'
    };
});

prodAndes.directive('toolbarConsultaEtapas', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/toolbar-consulta-etapas.html',
        controller:function(){
            this.tab=0;
            this.selectTab=function(setTab){
                console.log("Select Tab Etapas "+setTab  );
                this.tab=setTab;

            };
            this.isSelected=function(tabParam){
                return this.tab===tabParam;
            };
        },
        controllerAs:'toolbarConsultaEtapasCtrl'
    };
});

prodAndes.directive('toolbarConsultaProveedores', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/toolbar-consulta-proveedores.html',
        controller:function(){
            this.tab=0;
            this.selectTab=function(setTab){
                this.tab=setTab;
            };
            this.isSelected=function(tabParam){
                return this.tab===tabParam;
            };
        },
        controllerAs:'toolbarConsultaProveedoresCtrl'
    };
});

prodAndes.directive('toolbarConsultaSuministros', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/toolbar-consulta-suministros.html',
        controller:function(){
            this.tab=0;
            this.selectTab=function(setTab){
                this.tab=setTab;
            };
            this.isSelected=function(tabParam){
                return this.tab===tabParam;
            };
        },
        controllerAs:'toolbarConsultaSuministrosCtrl'
    };
});

    prodAndes.directive('consultarProductosForm', function(){
        return{
            restrict:'E',
            templateUrl: 'partials/consultar-productos-form.html',
            controller: ['$http',function($http){
                var self = this;


                self.order='';
                self.consulta = {};
                self.productos = [];

                this.isFull=function(){
                    return self.productos.length>0;
                };

                this.darOrder = function(){

                    console.log('Dar order '+JSON.stringify(self.consulta.order));
                    return self.consulta.order;
                }

                this.enviarConsulta=function(consultaParam,criterio){

                    self.productos = [];
                    self.order='';

                    console.log("Criterio "+criterio)
                    self.consulta = consultaParam,
                    self.consulta.Criterio = criterio;
                    self.order=self.consulta.order;

                    console.log('Form consulta '+JSON.stringify(self.consulta));
                    $http.post('http://localhost:8080/backend/Servicios/consultarProductos' , self.consulta).success(function(data){

                        console.log("Consultar productos "+JSON.stringify(data));
                        self.productos=data;
                        console.log("Consultar productos 2"+JSON.stringify(self.productos));
                        self.consulta={};
                    });


                };
            }],
            controllerAs:'consultarProductosCtrl'
        };
    });
    
    prodAndes.directive('consultarClientesForm', function(){
        return{
            restrict:'E',
            templateUrl: 'partials/consultar-clientes-form.html',
            controller: ['$http',function($http){
                var self = this;

                self.consulta = {};
                self.clientes = [];

                this.isFull=function(){
                    return self.clientes.length>0;
                };

                this.enviarConsulta=function(consultaParam,criterio){

                    self.clientes = [];
                    self.order='';

                    console.log("LOG - - - - Criterio "+criterio)
                    self.consulta = consultaParam,
                    self.consulta.Criterio = criterio;
                    self.order=self.consulta.order;

                    console.log('Form consulta '+JSON.stringify(self.consulta));
                    $http.post('http://localhost:8080/backend/Servicios/consultarClientes' , self.consulta).success(function(data){

                        console.log("Consultar clientes "+JSON.stringify(data));
                        self.clientes=data;
                        console.log("Consultar clientes 2"+JSON.stringify(self.clientes));
                        self.consulta={};
                    });


                };
            }],
            controllerAs:'consultarClientesCtrl'
        };
    });
    
    prodAndes.directive('consultarRf10Form', function(){
        return{
            restrict:'E',
            templateUrl: 'partials/consultar-rf10-form.html',
            controller: ['$http',function($http){
                var self = this;

                self.consulta = {};
                self.pedidos = [];

                this.isFull=function(){
                    return self.pedidos.length>0;
                };

                this.enviarConsulta=function(consultaParam){

                    self.pedidos = [];
                    self.order='';

                    self.consulta = consultaParam;

                    console.log('Form consulta '+JSON.stringify(self.consulta));
                    $http.post('http://localhost:8080/backend/Servicios/consultarPedidosRFC10' , self.consulta).success(function(data){

                        console.log("Consultar clientes "+JSON.stringify(data));
                        self.pedidos=data;
                        console.log("Consultar clientes 2"+JSON.stringify(self.pedidos));
                        self.consulta={};
                    });


                };
            }],
            controllerAs:'consultarRf10Ctrl'
        };
    });
    
    prodAndes.directive('consultarRf11Form', function(){
        return{
            restrict:'E',
            templateUrl: 'partials/consultar-rf11-form.html',
            controller: ['$http',function($http){
                var self = this;

                self.consulta = {};
                self.pedidos = [];

                this.isFull=function(){
                    return self.pedidos.length>0;
                };

                this.enviarConsulta=function(consultaParam){

                    self.pedidos = [];
                    self.order='';

                    self.consulta = consultaParam;

                    console.log('Form consulta '+JSON.stringify(self.consulta));
                    $http.post('http://localhost:8080/backend/Servicios/consultarPedidosRFC11' , self.consulta).success(function(data){

                        console.log("Consultar clientes "+JSON.stringify(data));
                        self.pedidos=data;
                        console.log("Consultar clientes 2"+JSON.stringify(self.pedidos));
                        self.consulta={};
                    });


                };
            }],
            controllerAs:'consultarRf11Ctrl'
        };
    });
    
    prodAndes.directive('consultarPedidosForm', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/consultar-pedidos-form.html',
        controller: ['$http',function($http){
            var self = this;


            self.order='';
            self.consulta = {};
            self.pedidos = [];
            self.pedido={};
            self.pedidoSelected=[];
            this.getSelected = function(){
                return self.pedidoSelected[0];
            }
            this.isFull=function(){
                return self.pedidos.length>0;
            };

            this.darOrder = function(){

                console.log('Dar order '+JSON.stringify(self.consulta.order));
                return self.consulta.order;
            };
            this.limpiarSelected=function(){

                console.log("limpiar selected");
                self.pedidoSelected=[];
            }

            this.isSelected=function(){

                return self.pedidoSelected.length>0;
            }

            this.enviarConsulta=function(consultaParam,criterio){


                self.pedidos = [];
                self.order='';
                self.pedidoSelected = [];
                console.log("Criterio "+criterio)
                self.consulta = consultaParam,
                self.consulta.Criterio = criterio;
                self.order=self.consulta.order;

                console.log('Form consulta '+JSON.stringify(self.consulta));
                $http.post('http://localhost:8080/backend/Servicios/consultarPedidos' , self.consulta).success(function(data){

                    console.log("Consultar pedidos "+JSON.stringify(data));
                    self.pedidos=data;
                    console.log("Consultar pedidos 2"+JSON.stringify(self.pedidos));
                    self.consulta={};
                });


            };
            this.cancelPedido=function(idPedido){

                alert('alert cancel pedido '+idPedido)    
                self.pedido = {};
                self.pedido.id_pedido = idPedido;
                console.log('Form pedido cancelar '+JSON.stringify(self.pedido));
                $http.post('http://localhost:8080/backend/Servicios/cancelarPedido'
                    , self.pedido).success(function(data){

                        console.log('Data '+JSON.stringify(data));
                        alert("Se ha cancelado su pedido");

                });
            };
            this.verPedido=function(idPedido){

                console.log('selectPedido '+idPedido)
                self.pedidoSelected[0] = idPedido;
                console.log('selectPedido 2' +self.pedidoSelected)
                self.consulta.id_pedido = idPedido;
                $http.post('http://localhost:8080/backend/Servicios/verPedido' , self.consulta).success(function(data){

                    console.log("VerPedido pedidos "+JSON.stringify(data));
                    self.pedidoSelected[0] = data;
                    self.consulta={};
                });
                self.consulta={};
            }
            
        }],
        controllerAs:'consultarPedidosCtrl'
    };
});

prodAndes.directive('consultarEtapasForm', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/consultar-etapas-form.html',
        controller: ['$http',function($http){
            var self = this;


            self.order='';
            self.consulta = {};
            self.etapas = [];
            self.etapa={};
            self.etapaSelected=[];
            this.getSelected = function(){
                return self.etapaSelected[0];
            }
            this.isFull=function(){
                return self.etapas.length>0;
            };

            this.limpiarSelected=function(){

                console.log("limpiar selected");
                self.etapaSelected=[];
                self.etapas=[];
            }

            this.isSelected=function(){

                return self.etapaSelected.length>0;
            }

            this.enviarConsulta=function(consultaParam,criterio){


                self.etapas = [];
                
                self.etapaSelected = [];
                console.log("Criterio "+criterio)
                self.consulta = consultaParam,
                self.consulta.criterio = criterio;
                

                console.log('Form consulta '+JSON.stringify(self.consulta));
                $http.post('http://localhost:8080/backend/Servicios/consultarEtapasRangoFechaRFC8y9' , self.consulta).success(function(data){

                    console.log("Consultar etapas "+JSON.stringify(data));
                    self.etapas=data;
                    console.log("Consultar etapas 2"+JSON.stringify(self.etapas));
                    self.consulta={};
                });


            };
            
            
        }],
        controllerAs:'consultarEtapasCtrl'
    };
});

prodAndes.directive('consultarProveedoresForm', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/consultar-proveedores-form.html',
        controller: ['$http',function($http){
            var self = this;


            self.order='';
            self.consulta = {};
            self.proveedores = [];
            self.proveedor={};
            self.proveedorSelected=[];
            this.isFull=function(){
                return self.proveedores.length>0;
            };


            this.isSelected=function(){

                return self.proveedorSelected.length>0;
            }

            this.enviarConsulta=function(consultaParam,criterio){

                self.proveedores = [];
                self.order='';
                self.proveedorSelected=[];
                console.log("Criterio "+criterio)
                self.consulta = consultaParam,
                self.consulta.Criterio = criterio;

                console.log('Form consulta '+JSON.stringify(self.consulta));
                $http.post('http://localhost:8080/backend/Servicios/consultarProveedores' , self.consulta).success(function(data){

                    console.log("Consultar proveedores "+JSON.stringify(data));
                    self.proveedores=data;
                    console.log("Consultar proveedores 2"+JSON.stringify(self.proveedores));
                    self.consulta={};
                });


            };
            this.verProveedor=function(idProveedor){

                console.log('verProveedor '+idProveedor)
                self.proveedorSelected[0] = idProveedor;
                console.log('verProveedor 2' +self.proveedorSelected)
                self.consulta.id_proveedor = idProveedor;
                $http.post('http://localhost:8080/backend/Servicios/verProveedor' , self.consulta).success(function(data){

                    console.log("VerProveedor "+JSON.stringify(data));
                    self.proveedorSelected[0] = data;
                    self.consulta={};
                });
                self.consulta={};
            }
            
        }],
        controllerAs:'consultarProveedoresCtrl'
    };
});

prodAndes.directive('listaProductosConsulta', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/lista-productos-consulta.html',
        controller:function(){

        },
        controllerAs:'listaProductosConsulta'
    };
});

prodAndes.directive('listaClientesConsulta', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/lista-clientes-consulta.html',
        controller:function(){

        },
        controllerAs:'listaClientesConsulta'
    };
});

prodAndes.directive('listaRf10Consulta', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/lista-rf10-consulta.html',
        controller:function(){

        },
        controllerAs:'listaRf10Consulta'
    };
});


prodAndes.directive('listaRf11Consulta', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/lista-rf11-consulta.html',
        controller:function(){

        },
        controllerAs:'listaRf11Consulta'
    };
});

prodAndes.directive('listaPedidosConsulta', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/lista-pedidos-consulta.html',
        controller:function(){

        },
        controllerAs:'listaPedidosConsulta'
    };
});
prodAndes.directive('listaEtapasConsulta', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/lista-etapas-consulta.html',
        controller:function(){

        },
        controllerAs:'listaEtapasConsulta'
    };
});
prodAndes.directive('listaProveedoresConsulta', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/lista-proveedores-consulta.html',
        controller:function(){

        },
        controllerAs:'listaPedidosConsulta'
    };
});


prodAndes.directive('consultarMateriasForm', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/consultar-materias-form.html',
        controller: ['$http',function($http){
            var self = this;

            self.order='';
            self.consulta = {};
            self.materias = [];

            this.isFull=function(){
                return self.materias.length>0;
            };


            this.enviarConsulta=function(consultaParam,criterio){

                self.materias = [];
                self.order = '';
                console.log("Criterio MAterias "+criterio)
                self.consulta = consultaParam,
                self.consulta.Criterio = criterio;
                self.order = self.consulta.order;
                console.log('Form consulta materias '+JSON.stringify(self.consulta));
                $http.post('http://localhost:8080/backend/Servicios/consultarMateriasPrimas', self.consulta).success(function(data){

                    console.log("Consultar materias "+JSON.stringify(data));
                    self.materias=data;
                    console.log("Consultar materias 2"+JSON.stringify(self.materias));
                    self.consulta={};
                });


            };
        }],
        controllerAs:'consultarMateriasCtrl'
    };
});

prodAndes.directive('listaMateriasConsulta', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/lista-materias-consulta.html',
        controller:function(){

        },
        controllerAs:'listaMateriasConsulta'
    };
});

prodAndes.directive('consultarComponentesForm', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/consultar-componentes-form.html',
        controller: ['$http',function($http){
            var self = this;

            self.order='';
            self.consulta = {};
            self.componentes = [];

            this.isFull=function(){
                return self.componentes.length>0;
            };


            this.enviarConsulta=function(consultaParam,criterio){

                self.componentes = [];
                self.order='';
                console.log("Criterio "+criterio)
                self.consulta = consultaParam,
                self.consulta.Criterio = criterio;
                self.order= self.consulta.order;
                console.log('Form consulta '+JSON.stringify(self.consulta));
                $http.post('http://localhost:8080/backend/Servicios/consultarComponentes', self.consulta).success(function(data){

                    console.log("Consultar Componentes "+JSON.stringify(data));
                    self.componentes=data;
                    console.log("Consultar Componentes 2"+JSON.stringify(self.componentes));
                    self.consulta={};
                });


            };
        }],
        controllerAs:'consultarComponentesCtrl'
    };
});

prodAndes.directive('listaComponentesConsulta', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/lista-componentes-consulta.html',
        controller:function(){

        },
        controllerAs:'listaComponentesConsulta'
    };
});
prodAndes.directive('registrarProveedorForm', function(){
    return{
        restrict:'E',
        templateUrl:'partials/registrar-proveedor-form.html',
        controller: ['$http',function($http){
            var self=this;
            self.proveedor={};
            this.addProveedor=function(proveedorParam){

             self.proveedor = proveedorParam,
             console.log('Que es esto '+JSON.stringify(proveedorParam));console.log('Form pedido '+JSON.stringify(self.proveedor));
             $http.post('http://localhost:8080/backend/Servicios/registrarProveedor'
                 , self.proveedor).success(function(data){
                     self.proveedor={};
                 });
             };
         }],
         controllerAs:'registrarProveedorCtrl'
     };
 });

prodAndes.directive('registrarLlegadaMaterialForm', function(){
    return{
        restrict:'E',
        templateUrl:'partials/registrar-llegada-material-form.html',
        controller: ['$http',function($http){
            var self=this;
            self.llegada={};
            this.addLlegadaMaterial=function(llegadaParam){

             self.llegada = llegadaParam,
             console.log('Que es esto '+JSON.stringify(llegadaParam));console.log('Form pedido '+JSON.stringify(self.llegada));
             $http.post('http://localhost:8080/backend/Servicios/registrarLlegadaDeMaterial'
                 , self.llegada).success(function(data){
                     alert("Respuesta "+data.Respuesta);
                     self.llegada={};
                 });
             };
         }],
         controllerAs:'registrarLlegadaMaterialCtrl'
     };
 });

prodAndes.directive('registrarLlegadaComponenteForm', function(){
    return{
        restrict:'E',
        templateUrl:'partials/registrar-llegada-componente-form.html',
        controller: ['$http',function($http){
            var self=this;
            self.llegada={};
            this.addLlegadaComponente=function(llegadaParam){

             self.llegada = llegadaParam,
             console.log('Que es esto '+JSON.stringify(llegadaParam));console.log('Form pedido '+JSON.stringify(self.llegada));
             $http.post('http://localhost:8080/backend/Servicios/registrarLlegadaDeComponentes'
                 , self.llegada).success(function(data){
                     alert("Respuesta "+data.Respuesta);
                     self.llegada={};
                 });
             };
         }],
         controllerAs:'registrarLlegadaComponenteCtrl'
     };
 });

prodAndes.directive('registrarEjecucionEtapaForm', function(){
    return{
        restrict:'E',
        templateUrl:'partials/registrar-ejecucion-etapa-form.html',
        controller: ['$http',function($http){
            var self=this;
            self.ejecucionEtapa={};
            this.addEjecucionEtapa=function(EjecucionEtapaP){

             self.ejecucionEtapa = EjecucionEtapaP,
             console.log('Form pedido '+JSON.stringify(self.ejecucionEtapa));
             $http.post('http://localhost:8080/backend/Servicios/registrarEjecucionEtapa'
                 , self.ejecucionEtapa).success(function(data){
                     alert("Respuesta "+data.Respuesta);
                     self.ejecucionEtapa={};
                 });
             };
         }],
         controllerAs:'registrarEjecucionEtapaCtrl'
     };
 });
 
 prodAndes.directive('activarEstacionForm', function(){
    return{
        restrict:'E',
        templateUrl:'partials/activar-estacion-form.html',
        controller: ['$http',function($http){
            var self=this;
            self.activarEstacion={};
            this.addActivarEstacion=function(ActivarEstacionP){

             self.activarEstacion = ActivarEstacionP,
             console.log('Form pedido '+JSON.stringify(self.activarEstacion));
             $http.post('http://localhost:8080/backend/Servicios/activarEstacion'
                 , self.activarEstacion).success(function(data){
                     alert("Respuesta "+data.Respuesta);
                     self.activarEstacion={};
                 });
             };
         }],
         controllerAs:'activarEstacionCtrl'
     };
 });
 
  prodAndes.directive('desactivarEstacionForm', function(){
    return{
        restrict:'E',
        templateUrl:'partials/desactivar-estacion-form.html',
        controller: ['$http',function($http){
            var self=this;
            self.desactivarEstacion={};
            this.addDesactivarEstacion=function(DesActivarEstacionP){

             self.desactivarEstacion = DesActivarEstacionP,
             console.log('A desactivar '+JSON.stringify(self.desactivarEstacion));
             $http.post('http://localhost:8080/backend/Servicios/desactivarEstacion'
                 , self.desactivarEstacion).success(function(data){
                     alert("Respuesta "+data.Respuesta);
                     self.desactivarEstacion={};
                 });
             };
         }],
         controllerAs:'desactivarEstacionCtrl'
     };
 });

prodAndes.directive('etapaMayorMovimientoForm', function(){

    return{
        restrict:'E',
        templateUrl: 'partials/etapa-mayor-movimiento-form.html',
        controller: ['$http',function($http){
            var self = this;

            self.consulta = {};   

            this.enviarConsulta=function(consultaParam){


                self.consulta = consultaParam,
                console.log('Form consulta '+JSON.stringify(self.consulta));
                $http.post('http://localhost:8080/backend/Servicios/consultarEtapaProduccionMayorMovimiento', self.consulta).success(function(data){

                    console.log("Etapa mayor movimiento"+JSON.stringify(data));
                    alert("La etapa más activa es: "+data.CODIGO_SECUENCIA+" con "+data.CUENTA+" ejecuciones.");
                    self.consulta={};
                });


            };
        }],
        controllerAs:'etapaMayorMovimientoCtrl'
    };
});

prodAndes.directive('operarioMasActivoForm', function(){

    return{
        restrict:'E',
        templateUrl: 'partials/operario-mas-activo-form.html',
        controller: ['$http',function($http){
            var self = this;

            self.consulta = {};   

            this.enviarConsulta=function(consultaParam){


                self.consulta = consultaParam,
                console.log('Form consulta '+JSON.stringify(self.consulta));
                $http.post('http://localhost:8080/backend/Servicios/operarioMasActivo', self.consulta).success(function(data){

                    console.log("Operario mas activo en etapa:"+JSON.stringify(data));
                    alert(JSON.stringify(data));
                    self.consulta={};
                });


            };
        }],
        controllerAs:'operarioMasActivoCtrl'
    };
});
prodAndes.directive('verPedidoForm', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/ver-pedido-form.html',
        controller:function(){

        },
        controllerAs:'verPedidoCtrl'
    };
});
prodAndes.directive('verProveedorForm', function(){
    return{
        restrict:'E',
        templateUrl: 'partials/ver-proveedor-form.html',
        controller:function(){

        },
        controllerAs:'verProveedorCtrl'
    };
});
})();

