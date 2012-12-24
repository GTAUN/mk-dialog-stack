/**
 * Copyright (C) 2012 MK124
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package net.gtaun.mk.shoebill.dialog;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;

import net.gtaun.shoebill.SampObjectStore;
import net.gtaun.shoebill.constant.DialogStyle;
import net.gtaun.shoebill.event.PlayerEventHandler;
import net.gtaun.shoebill.event.dialog.DialogCancelEvent;
import net.gtaun.shoebill.event.dialog.DialogCancelEvent.DialogCancelType;
import net.gtaun.shoebill.event.dialog.DialogResponseEvent;
import net.gtaun.shoebill.event.player.PlayerConnectEvent;
import net.gtaun.shoebill.event.player.PlayerDisconnectEvent;
import net.gtaun.shoebill.object.Dialog;
import net.gtaun.shoebill.object.Player;
import net.gtaun.shoebill.proxy.MethodInterceptor;
import net.gtaun.shoebill.proxy.MethodInterceptor.Helper;
import net.gtaun.shoebill.proxy.MethodInterceptor.Interceptor;
import net.gtaun.shoebill.proxy.MethodInterceptor.InterceptorPriority;
import net.gtaun.shoebill.proxy.ProxyManager;
import net.gtaun.shoebill.resource.Plugin;
import net.gtaun.shoebill.resource.ResourceDescription;
import net.gtaun.util.event.Event;
import net.gtaun.util.event.EventManager.HandlerPriority;
import net.gtaun.util.event.ManagedEventManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 对话框堆栈插件主类。
 * 
 * @author MK124
 */
public class DialogStackPlugin extends Plugin
{
	public static final Logger LOGGER = LoggerFactory.getLogger(DialogStackPlugin.class);
	
	
	private ManagedEventManager managedEventManager;
	
	private MethodInterceptor showDialogMethodInterceptor;
	private Map<Player, Deque<DialogContext>> playerDialogStacks;
	private HashSet<Event> dispatchingEvents;
	
	
	public DialogStackPlugin()
	{
		
	}
	
	@Override
	protected void onEnable() throws Throwable
	{
		managedEventManager = new ManagedEventManager(getEventManager());
		playerDialogStacks = new WeakHashMap<>();
		dispatchingEvents = new HashSet<>();
		
		SampObjectStore store = getShoebill().getSampObjectStore();
		for (Player player : store.getPlayers())
		{
			playerDialogStacks.put(player, new ArrayDeque<DialogContext>());
		}
		
		managedEventManager.registerHandler(PlayerConnectEvent.class, playerEventHandler, HandlerPriority.MONITOR);
		managedEventManager.registerHandler(PlayerDisconnectEvent.class, playerEventHandler, HandlerPriority.BOTTOM);
		managedEventManager.registerHandler(DialogCancelEvent.class, playerEventHandler, HandlerPriority.MONITOR);
		managedEventManager.registerHandler(DialogResponseEvent.class, playerEventHandler, HandlerPriority.MONITOR);
		
		Method methodShowDialog = Player.class.getMethod("showDialog", Dialog.class, DialogStyle.class, String.class, String.class, String.class, String.class);
		
		ProxyManager proxyManager = getShoebill().getGlobalProxyManager();
		showDialogMethodInterceptor = proxyManager.createMethodInterceptor(methodShowDialog, showDialogInterceptor, InterceptorPriority.BOTTOM);
		
		ResourceDescription desc = getDescription();
		LOGGER.info("{} {} (Build {}) Enabled.", desc.getName(), desc.getVersion(), desc.getBuildNumber());
	}
	
	@Override
	protected void onDisable() throws Throwable
	{
		showDialogMethodInterceptor.cancel();
		managedEventManager.cancelAll();
		playerDialogStacks.clear();
		
		ResourceDescription desc = getDescription();
		LOGGER.info("{} {} Disabled.", desc.getName(), desc.getVersion());
	}
	
	private Interceptor showDialogInterceptor = new Interceptor()
	{
		@Override
		public Object intercept(Helper helper, Method method, Object obj, Object[] args) throws Throwable
		{
			Player player = (Player) obj;
			Deque<DialogContext> stack = playerDialogStacks.get(player);
			
			Object ret = helper.invokeLower(obj, args);
			stack.push(new DialogContext((Dialog) args[0], (DialogStyle) args[1], (String) args[2], (String) args[3], (String) args[4], (String) args[5]));
			
			return ret;
		}
	};
	
	private void showStackDialog(Player player)
	{
		Deque<DialogContext> stack = playerDialogStacks.get(player);
		if (stack.isEmpty()) return;
		
		DialogContext dialogContext = stack.pop();
		
		showDialogMethodInterceptor.setEnabled(false);
		player.showDialog(dialogContext.getDialog(), dialogContext.getStyle(), dialogContext.getCaption(), dialogContext.getText(), dialogContext.getButton1(), dialogContext.getButton2());
		showDialogMethodInterceptor.setEnabled(true);
	}
	
	private PlayerEventHandler playerEventHandler = new PlayerEventHandler()
	{
		protected void onPlayerConnect(PlayerConnectEvent event)
		{
			Player player = event.getPlayer();
			playerDialogStacks.put(player, new ArrayDeque<DialogContext>());
		}
		
		protected void onPlayerDisconnect(PlayerDisconnectEvent event)
		{
			Player player = event.getPlayer();
			playerDialogStacks.remove(player);
		}
		
		protected void onPlayerDialogCancel(DialogCancelEvent event)
		{
			Player player = event.getPlayer();
			Dialog dialog = event.getDialog();
			
			if (event.getType() == DialogCancelType.OVERRIDE)
			{
				event.interrupt();
			}
			else if (event.getType() == DialogCancelType.CANCEL)
			{
				Deque<DialogContext> stack = playerDialogStacks.get(player);
				if (dialog == stack.peekFirst())
				{
					stack.pop();
					showStackDialog(player);
				}
			}
		}
		
		protected void onPlayerDialogResponse(DialogResponseEvent event)
		{
			if (dispatchingEvents.contains(event)) return;
			
			Dialog dialog = event.getDialog();
			Player player = event.getPlayer();
			int response = event.getResponse();
			int listitem = event.getListitem();
			String inputtext = event.getInputText();
			
			Deque<DialogContext> stack = playerDialogStacks.get(player);
			if (dialog == stack.peekFirst()) stack.pop();
			
			DialogResponseEvent responseEvent = new DialogResponseEvent(dialog, player, response, listitem, inputtext);
			
			dispatchingEvents.add(responseEvent);
			getEventManager().dispatchEvent(responseEvent, dialog, player);
			dispatchingEvents.remove(responseEvent);
			
			event.interrupt();
			if (responseEvent.getResponse() != 0) event.setProcessed();
			
			if (player.getDialog() == null)
			{
				showStackDialog(player);
			}
		}
	};
}
