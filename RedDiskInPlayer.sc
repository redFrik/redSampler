//redFrik

//--changes220116
//replace FlowLayout with Layout and fix background colors
//change from Dialog to FileDialog to better handle folders
//folder selection now remember last location
//progressbar now wrap around when loop is on
//--changes171223:
//rewrite to work with Qt and sc3.9
//--changes090829:
//drag and drop from finder using a dragsink
//--changes090613:
//no longer use the GUI redirect (GUI.button.new became Button)
//--changes090123:
//changed boxColor_ to background_
//ctrl click to reset volume to 0db

//--todo:
//* colours and font from skin?

RedDiskInPlayer {
	var <sampler, <isPlaying= false, <soundFiles, <win;
	var playIndex, bgcol, fgcol, incdecTask;
	var incView, decView, progressView, infoView, volNumView, volSldView;
	var envNumView, envSldView, listView, busView, loopView, filterView;
	var pathPrev;  //remember last location

	*new {|server, bus= 0, numItems= 10|
		^super.new.initRedDiskInPlayer(server, bus, numItems);
	}

	initRedDiskInPlayer {|argServer, argBus, argNumItems|
		var server= argServer ?? Server.default;
		var w= 160;  //widget max width
		var h= 18;  //widget height
		var fnt= Font(Font.defaultMonoFace, 9);
		var volSpec= #[-90, 6, \db].asSpec;

		bgcol= Color.red(0.8);
		fgcol= Color.black;
		soundFiles= [];

		win= Window(
			this.class.name,
			Rect(500, 200, w+10, argNumItems*(fnt.size+2)+150),
			false
		);
		win.alpha_(0.9);
		win.view.background= bgcol;
		win.view.layout= VLayout(

			HLayout(
				volNumView= NumberBox().fixedWidth_(w*0.25)
				.background_(bgcol)
				.typingColor_(Color.white)
				.value_(0)
				.action_({|view|
					volSldView.value= volSpec.unmap(view.value);
					if(isPlaying, {sampler.amp= volNumView.value.dbamp});
				}),
				volSldView= Slider().minWidth_(w*0.6)
				.background_(bgcol)
				.knobColor_(fgcol)
				.orientation_(\horizontal)
				.value_(volSpec.unmap(0))
				.action_({|view|
					volNumView.value= volSpec.map(view.value).round(0.1);
					if(isPlaying, {sampler.amp= volNumView.value.dbamp});
				})
				.mouseUpAction_({|view, x, y, mod|
					if(mod.isCtrl, {
						{view.valueAction= volSpec.unmap(0)}.defer(0.1);
					});
				}),
				StaticText().string_("vol")
			),

			HLayout(
				envNumView= NumberBox().fixedWidth_(w*0.25)
				.background_(bgcol)
				.typingColor_(Color.white)
				.value_(0.05)
				.action_({|view|
					view.value= view.value.max(0);
					envSldView.value= (view.value/10).min(1);
				}),
				envSldView= Slider().minWidth_(w*0.6)
				.background_(bgcol)
				.knobColor_(fgcol)
				.orientation_(\horizontal)
				.action_({|view|
					envNumView.value= (view.value*10).round(0.1);
				}),
				StaticText().string_("env")
			),

			HLayout(
				busView= NumberBox().fixedWidth_(w*0.25)
				.background_(bgcol)
				.typingColor_(Color.white)
				.value_(argBus)
				.action_({|view|
					view.value= view.value.asInteger.max(0);
				}),
				StaticText().string_("bus"),

				loopView= Button().minWidth_(w*0.4)
				.states_([["loop", fgcol, bgcol], ["loop", bgcol, fgcol]])
			),

			HLayout(
				incView= StaticText().string_("0:00"),
				decView= StaticText().string_("0:00.0")
			),

			progressView= MultiSliderView()
			.background_(bgcol)
			.indexIsHorizontal_(false)
			.editable_(false)
			.indexThumbSize_(h)
			.valueThumbSize_(0)
			.isFilled_(true)
			.canFocus_(false)
			.value_([0]),

			infoView= StaticText(),

			listView= ListView()
			.canReceiveDragHandler_({View.currentDrag.isString or:{
				View.currentDrag.isKindOf(Array)
			}})
			.receiveDragHandler_({|view|
				var drag= View.currentDrag;
				if(drag.isString, {
					if(drag.last==$/, {
						soundFiles= (drag++"*").pathMatch.collect{|x| SoundFile(x)};  //folder of files
					}, {
						soundFiles= [SoundFile(drag)];  //single file
					});
				}, {
					soundFiles= drag.collect{|x| SoundFile(x)};  //selected files
				});
				if(filterView.value>0, {
					soundFiles= soundFiles.select{|x| x.numChannels==filterView.value};
				});
				soundFiles.do{|x, i|
					sampler.prepareForPlay(i, x.path);
				};
				view.items= soundFiles.collect{|x| x.path.basename};
				this.prUpdateInfo(0);
			})
			.background_(bgcol)
			.hiliteColor_(bgcol)
			.selectedStringColor_(Color.white)
			.action_({|view|
				this.prUpdateInfo(view.value);
				if(isPlaying, {
					this.prStopFunc(view);
				});
			})
			.enterKeyAction_({|view|
				if(soundFiles[view.value].notNil, {
					if(isPlaying, {
						this.prStopFunc(view);
					}, {
						this.prPlayFunc(view);
					});
				});
			}),

			HLayout(
				Button().minWidth_(w*0.4)
				.states_([["folder...", fgcol, bgcol]])
				.action_({
					if(sampler.notNil, {sampler.free});
					FileDialog({|path|
						if(File.type(path)==\regular, {path= path.dirname});
						pathPrev= path.dirname;
						soundFiles= SoundFile.collect(path+/+"*");
						if(filterView.value>0, {
							soundFiles= soundFiles.select{|x| x.numChannels==filterView.value};
						});
						soundFiles.do{|x, i|
							sampler.prepareForPlay(i, x.path);
						};
						listView.items= soundFiles.collect{|x| x.path.basename};
						this.prUpdateInfo(0);
					}, nil, 0, 0, true, pathPrev);
					listView.focus;
				}).focus,
				filterView= NumberBox().fixedWidth_(w*0.2)
				.background_(bgcol)
				.typingColor_(Color.white)
				.value_(0)
				.action_({|view|
					view.value= view.value.max(0).round;
				}),
				StaticText().string_("filter")
			)
		).margins_(4).spacing_(4);

		win.view.children.do{|v|
			if(v.respondsTo('font_'), {v.font_(fnt)});
			if(v.isKindOf(ListView).not, {v.maxHeight_(h)});
		};

		Routine.run{
			server.bootSync;
			sampler= RedDiskInSampler(server);
			server.sync;

			CmdPeriod.doOnce({if(win.isClosed.not, {win.close})});

			defer{
				win.onClose= {incdecTask.stop; sampler.free};
				win.front;
			};
		};
	}

	bus {
		^busView.value;
	}

	free {
		win.close;
	}

	//--private
	prPlayFunc {|view|
		isPlaying= true;
		playIndex= view.value;
		view.selectedStringColor_(Color.red);
		view.hiliteColor_(fgcol);
		sampler.play(
			playIndex,
			envNumView.value,
			nil,
			envNumView.value,
			volNumView.value.dbamp,
			busView.value,
			nil,
			loopView.value
		);
		incdecTask.stop;
		incdecTask= Routine({
			var startTime= SystemClock.seconds;
			var stopTime= sampler.length(playIndex);
			decView.string= stopTime.asTimeString;
			inf.do{
				var now= SystemClock.seconds-startTime;
				incView.string= now.round.asTimeString;
				10.do{
					0.1.wait;
					now= SystemClock.seconds-startTime;
					progressView.value= [(now/stopTime)%1];
				};
			};
		}).play(AppClock);
	}

	prStopFunc {|view|
		isPlaying= false;
		if(sampler.playingKeys.notEmpty, {
			sampler.stop(playIndex, envNumView.value);
		});
		incdecTask.stop;
		view.selectedStringColor_(Color.white);
		view.hiliteColor_(bgcol);
		progressView.value= #[0];
		incView.string= "0:00";
	}

	prUpdateInfo {|index|
		var sf= soundFiles[index];
		if(sf.notNil, {
			decView.string= sf.duration.asTimeString(0.01);
			infoView.string= "".scatList([
				sf.numChannels,
				sf.headerFormat,
				sf.sampleFormat,
				sf.sampleRate
			]);
		});
	}
}
