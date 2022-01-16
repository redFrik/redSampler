//these classes are part of the RedSample package / redFrik, gnu gpl v2
//uses an already created buffer in ram

RedSamp : RedSampler {
	prepareForPlay {|key, buffer, startFrame= 0, numFrames|
		if(server.serverRunning.not, {(this.class.asString++": server not running").error; this.halt});
		keys.put(key, {  //associate key with array of voice objects
			this.prCreateVoice(buffer, startFrame, numFrames);
		}.dup(overlaps));
	}
	prCreateVoice {|buffer, startFrame, argNumFrames|
		var len;
		if(argNumFrames.notNil, {
			len= argNumFrames/buffer.sampleRate;
		}, {
			len= buffer.numFrames-startFrame/buffer.sampleRate;
		});
		^RedSampVoice(server, buffer, buffer.numChannels, startFrame, argNumFrames, len);
	}
}

RedSampVoice : RedSamplerVoice {
	prAllocBuffer {|action|
		var num= numFrames ? path.numFrames;
		server.bind{
			buffer= Buffer.alloc(server, num, channels);
			server.sync;
			path.copyData(buffer, 0, startFrame, num);
			server.sync;
			action.value;
		};
	}
}
